package com.leezer.truman;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {


    public static void main(String args[]){
        try {
            Trumanizer trumanizer = new Trumanizer(new File(""),new File(""),60,80);
            BufferedImage bu = trumanizer.trumanize();
            File outputfile = new File("");
            ImageIO.write(bu, "jpg", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}