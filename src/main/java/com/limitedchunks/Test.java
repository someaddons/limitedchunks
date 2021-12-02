package com.limitedchunks;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Test
{
    private Test()
    {
    }

    final static Random r = new Random();

    public static void main(String[] args) throws IOException, InterruptedException
    {

        while (true)
        {
            download("https://edge.forgecdn.net/files/3238/559/limitedchunks-1.9.jar", "E:\\limitedchunks.jar");
            Thread.sleep(1000000 + r.nextInt(3000));
        }
        // curl "https://edge.forgecdn.net/files/3238/559/limitedchunks-1.9.jar" -H "authority: edge.forgecdn.net" -H "upgrade-insecure-requests: 1" -H "user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.122 Safari/537.36" -H "sec-fetch-dest: document" -H "accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9" -H "sec-fetch-site: cross-site" -H "sec-fetch-mode: navigate" -H "sec-fetch-user: ?1" -H "referer: https://www.curseforge.com/minecraft/mc-mods/limited-chunkloading/files" -H "accept-language: en-US,en;q=0.9" --compressed
    }

    public static void download(String search, String path) throws IOException
    {

        // This will get input data from the server
        InputStream inputStream = null;

        // This will read the data from the server;
        OutputStream outputStream = null;

        try
        {
            // This will open a socket from client to server
            URL url = new URL(search);

            // This user agent is for if the server wants real humans to visit
            String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.1" + (r.nextInt(20) + 1) + " Safari/537.36";

            // This socket type will allow to set user_agent
            URLConnection con = url.openConnection();
            con.setRequestProperty("authority", "edge.forgecdn.net");
            con.setRequestProperty("upgrade-insecure-requests", "1");
            con.setRequestProperty("sec-fetch-dest", "document");
            con.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            con.setRequestProperty("sec-fetch-site", "cross-site");
            con.setRequestProperty("sec-fetch-mode", "navigate");
            con.setRequestProperty("sec-fetch-user", "?1");
            con.setRequestProperty("referer", " https://www.curseforge.com/minecraft/mc-mods/limited-chunkloading/files");
            con.setRequestProperty("accept-language", "en-US,en;q=0.9");

            // Setting the user agent
            con.setRequestProperty("user-agent", USER_AGENT);

            //Getting content Length
            int contentLength = con.getContentLength();
            Logger.getLogger("s").info("File contentLength = " + contentLength + " bytes");


            // Requesting input data from server
            inputStream = con.getInputStream();

            // Open local file writer
            outputStream = new FileOutputStream(path);

            // Limiting byte written to file per loop
            byte[] buffer = new byte[2048];

            // Increments file size
            int length;
            int downloaded = 0;

            // Looping until server finishes
            while ((length = inputStream.read(buffer)) != -1)
            {
                // Writing data
                outputStream.write(buffer, 0, length);
                downloaded += length;
            }
        }
        catch (Exception ex)
        {
            Logger.getLogger("s").log(Level.SEVERE, null, ex);
        }

        // closing used resources
        // The computer will not be able to use the image
        // This is a must
        outputStream.close();
        inputStream.close();
    }
}
