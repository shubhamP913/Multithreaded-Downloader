package io.shubham.DownloadManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
public class Main
{
    static BufferedReader in;
    public static void main(String[] args) throws IOException
    {
        in = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Start - 1\nPause - 2\nResume - 3\nExit - 4");
        System.out.println("Enter an option.");
        try
        {
            boolean flag = true;
            while(flag)
            {
                int option = Integer.parseInt(in.readLine());
                switch (option)
                {
                    case 1:
                        start();
                        break;
                    case 2:
                        pause();
                        break;
                    case 3:
                        resume();
                        break;
                    case 4:
                        System.exit(0);
                }

            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }
    static Downloader obj;
    public static void start() throws Exception {
        // System.out.println("Enter an url.");
        // String url = in.readLine();
        String path = "https://content.videvo.net/videvo_files/video/free/2020-04/originalContent/200401_Medical%206_01.mp4";
        URL url = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int fileLength = connection.getContentLength();
        System.out.println("Total file size:"+fileLength/1024/1024+"MB"+"    "+fileLength);
        obj = new Downloader(4, path, 1);
        obj.download();
    }

    public static void pause() {
        System.out.println("Download is Paused...");
        obj.pauseThread();
    }

    public static void resume() throws Exception
    {
        System.out.println("Resuming Download..");
        obj.resumeThread();
    }
    // public void cancel()
    // {
    //     obj.cancelThread();
    // }
}