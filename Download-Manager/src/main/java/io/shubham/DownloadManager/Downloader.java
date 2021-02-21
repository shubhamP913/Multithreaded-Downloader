package io.shubham.DownloadManager;

import java.io.File;
import java.io.*;
import java.util.*;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;

public class Downloader
{
    protected long startTime;
    protected static long endTime;
    protected int threadCount;
    protected String path;
    protected static int fileLength;
    protected static int downloaded=0;
    protected List<DownloadThread> threadList;
    protected static int state;
    public Downloader(int threadCount,String path,int state)
    {
        this.threadCount = threadCount;
        this.path = path;
        this.state = state;
        threadList = new ArrayList<DownloadThread>();
    }

    public void download() throws Exception
    {
        URL url = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        int code = connection.getResponseCode();
        if(code == 200)
        {
            fileLength = connection.getContentLength();
            //System.out.println("Total file size:"+fileLength/1024/1024+"MB"+"    "+fileLength);
            ExecutorService service = Executors.newFixedThreadPool(threadCount);
            RandomAccessFile randomAccessFile = new RandomAccessFile(new File(getFileName(url)), "rw");
            randomAccessFile.setLength(fileLength);

            int blockSize = fileLength/threadCount;
            if(threadList.size()==0)
            {
                for(int threadId = 0; threadId < threadCount; threadId++)
                {
                    int startIndex = threadId * blockSize;
                    int endIndex = (threadId+1) * blockSize -1;
                    if(threadId == (threadCount - 1))
                    {
                        endIndex = fileLength - 1;
                    }
                    DownloadThread aThread = new DownloadThread(threadId, startIndex, endIndex);
                    threadList.add(aThread);
                    startTime = System.currentTimeMillis();
                    service.execute(aThread);
                }
                //service.shutdown();
            }
            else
            {
                for(DownloadThread dt : threadList)
                {
                    service.execute(dt);
                }
            }
            service.shutdown();
        }
    }

    //Download the thread
    private class DownloadThread implements Runnable{

        private int threadId;
        private int startIndex;
        private int endIndex;

        public DownloadThread(int threadId, int startIndex, int endIndex) {
            this.threadId = threadId;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public void run()
        {

            System.out.println("thread"+ threadId + "start download");
            try
            {
                URL url = new URL(path);
                File downThreadFile = new File("downThread_" + threadId+".dt");
                RandomAccessFile downThreadStream = null;
                if(downThreadFile.exists())
                {
                    downThreadStream = new RandomAccessFile(downThreadFile,"rwd");
                    String startIndex_str = downThreadStream.readLine();
                    if(null==startIndex_str || "".equals(startIndex_str))
                    {
                        this.startIndex = startIndex;
                    }
                    else
                    {
                        this.startIndex = Integer.parseInt(startIndex_str)-1;// Set the download starting point
                    }
                }
                else
                {
                    downThreadStream = new RandomAccessFile(downThreadFile,"rw");
                }

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setRequestProperty("Range", "bytes="+ startIndex + "-" + endIndex);

                System.out.println("Thread_ "+threadId + " The starting point for downloading is " + startIndex + "The download destination is:" + endIndex);

                if(connection.getResponseCode() == 206)
                {
                    BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    RandomAccessFile randomAccessFile = new RandomAccessFile(new File(getFileName(url)), "rw");
                    randomAccessFile.seek(startIndex);

                    byte[] buffer = new byte[1024];
                    int length = -1;
                    int total = 0;
                    while(state==1 && (length = inputStream.read(buffer,0,1024)) != -1)
                    {
                        randomAccessFile.write(buffer, 0, length);
                        total += length;
                        downloaded += length;
                        downThreadStream.seek(0);
                        downThreadStream.write((startIndex + total + "").getBytes("UTF-8"));
                    }

                    downThreadStream.close();
                    inputStream.close();
                    randomAccessFile.close();
                    if(state==1)
                    {
                        threadList.remove(this);
                        System.out.println("Thread "+threadId+" Downloaded  ");
                        System.out.println(getUseTime()+" "+getAverageSpeed());
                        cleanTemp(downThreadFile);// Delete temporary files
                    }
                }
                else
                {
                    System.out.println("Response code is" +connection.getResponseCode() + ". Server does not support multi-threaded downloads");
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    void pauseThread()
    {
        state = 2;
        System.out.println((downloaded*100)/fileLength+"%  downloaded. ");
    }
    void resumeThread() throws Exception
    {
        state = 1;

        download();
    }

    // Delete the temporary files generated by the thread
    private synchronized void cleanTemp(File file){
        file.delete();
    }

    private String getUseTime() {
        long endTime = System.currentTimeMillis();
        long userTime = endTime - startTime;
        long useMinute = userTime / 1000 / 60;
        long remainderSeconds = (userTime - (useMinute * 1000 * 60)) / 1000;
        return String.format("%s minutes %s seconds", useMinute, remainderSeconds);
    }

    private String getAverageSpeed()
    {
        long useTime = System.currentTimeMillis() - startTime;
        useTime = useTime > 0 ? useTime / 1000 : 1;
        return (fileLength / 1000 / useTime) + "KB/s";
    }
    private String getFileName(URL url){
        String filename = url.getFile();
        return filename.substring(filename.lastIndexOf("/")+1);
    }
}
