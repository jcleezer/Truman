package com.leezer.truman;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {


    public static void main(String args[]){
        try {
            Trumanizer trumanizer = new Trumanizer(new File("D:\\Users\\Jason\\Downloads\\IMG_5285.jpg"),new File("D:\\Users\\Jason\\Pictures\\2018-09"),30,30);
            BufferedImage bu = trumanizer.trumanize();
            File outputfile = new File("D:\\Users\\Jason\\Downloads\\image.jpg");
            ImageIO.write(bu, "jpg", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}