package com.leezer.truman;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.harium.storage.kdtree.KDTree;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class Trumanizer {

    private final File baseImageFile;
    private final Map<String,Sample> images;
    private final int gridWidth;
    private final int gridHeight;
    private final int fidelity;



    public Trumanizer(File baseImage, File imageDir, int width, int height, int fidelity) throws IOException {

        this.fidelity = fidelity;
        this.baseImageFile = baseImage;
        this.gridHeight = height;
        this.gridWidth = width;
        this.images = buildImageIndex(imageDir, fidelity);
    }

    public BufferedImage trumanize() throws IOException {
        final BufferedImage base = ImageIO.read(baseImageFile);
        int tileWidth = Math.round(base.getWidth()/gridWidth);
        int tileHeight = Math.round(base.getHeight()/gridHeight);
        final BufferedImage output = new BufferedImage(tileWidth*gridWidth,tileHeight*gridHeight,base.getType());


        final List<Pair<Integer,Integer>> grid = generateGrid(gridWidth,gridHeight);
        Collections.shuffle(grid);
        grid.parallelStream().forEach( p -> {
            final int x = p.getLeft();
            final int y = p.getRight();
            System.out.println("Working on " + x + "," + y);
            BufferedImage subImage = base.getSubimage(x * tileWidth, y * tileHeight, tileWidth, tileHeight);
            Sample sample = getSample(subImage);
            KDTree<File> index = buildKDTree();
            final File nearestNeighbor = index.nearest(toDoubleArray(sample));
            System.out.println("Found " + nearestNeighbor.getName() + " for " + x + ", " + y);
            removeImage(nearestNeighbor);
            final BufferedImage shrunkImage;
            try {
                shrunkImage = resizeImage(ImageIO.read(nearestNeighbor), tileWidth, tileHeight);
                System.out.println("Writing to output");
                for (int w = 0; w < shrunkImage.getWidth(); w++) {
                    for (int h = 0; h < shrunkImage.getHeight(); h++) {
                        output.setRGB((x * tileWidth) + w, (y * tileHeight) + h, shrunkImage.getRGB(w, h));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Write image to output


        });
//        for (int x = 0; x < gridWidth; x++){
//            for (int y = 0; y < gridHeight; y++){
//
//            }
//        }
        return output;
    }

    private List generateGrid(int gridWidth, int gridHeight) {
        List<Pair<Integer,Integer>> ret = new LinkedList();
        for (int x = 0; x < gridWidth; x++){
            for (int y = 0; y < gridHeight; y++){
                ret.add(Pair.of(x,y));
            }
        }
        return ret;
    }

    private KDTree<File> buildKDTree(){
        final KDTree<File> index = new KDTree<>(3 * fidelity * fidelity);
        for (Map.Entry<String,Sample> entries : images.entrySet()){
            Sample sample = entries.getValue();
            try {
                //insert throws an exception when key exists
                index.insert(toDoubleArray(sample), new File(entries.getKey()));
            } catch (RuntimeException e){
                //e.printStackTrace();
            }
        }
        return index;
    }

    private double[] toDoubleArray(Sample sample){
        double[] ret = new double[sample.getSamples().size()*3];
        for (int i =0; i < sample.getSamples().size(); i++){
            RGB rgb = sample.getSamples().get(i);
            int index = i*3;
            ret[index] = rgb.getRed();
            ret[index+1] = rgb.getGreen();
            ret[index+2] = rgb.getBlue();
        }
        return ret;
    }
    private void removeImage(File key) {
        images.remove(key.getAbsolutePath());
    }


    private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height){
        BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width,height, null);
        g.dispose();

        return resizedImage;
    }

    private Map<String,Sample> buildImageIndex(File imageDir, int fidelity) throws IOException {
        final Gson gson = new Gson();
        final Map<String, Sample> images = Maps.newConcurrentMap();
        final File persisted = new File(imageDir,"truman.json");
        Map<Integer,Map<String,Sample>> persistedRgbs = new HashMap<>();
        if (persisted.exists()){
            Type typeOfHashMap = new TypeToken<Map<String, List<Sample>>>() { }.getType();
            persistedRgbs = gson.fromJson(new FileReader(persisted),typeOfHashMap);
            if (persistedRgbs != null && persistedRgbs.containsKey(fidelity)) {
                images.putAll(persistedRgbs.get(fidelity));
            }
        }

        Files.walk(Paths.get(imageDir.getAbsolutePath())).parallel()
                .filter(Files::isRegularFile)
                .filter(f ->isValidPhotoType(f.toString()))
                .forEach( path -> {if (!images.containsKey(path.toString()))addToMap(images,path);});

        addNewImagesToPersisted(persistedRgbs,images);
        //Persist
        FileUtils.writeStringToFile(persisted, gson.toJson(persistedRgbs));
        return images;
    }

    private void addNewImagesToPersisted(Map<Integer,Map<String,Sample>> persisted, Map<String, Sample> images) {
        Map<String,Sample> persistedImages = persisted.get(fidelity);
        if (persistedImages == null){
            persisted.put(fidelity,images);
        } else {
            persistedImages.putAll(images);
        }

    }

    private boolean isValidPhotoType(String name){
        final String lower = name.toLowerCase();
        return lower.endsWith("jpg") || lower.endsWith("jpeg");
    }

    private void addToMap(Map<String, Sample> values, Path path)  {
        final File imageFile = path.toFile();

        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image != null) {
                Sample sample = getSample(image);
                System.out.println("Processing " + path.toFile().getName());
                values.put(path.toString(), sample);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Sample getSample(BufferedImage image){
        int width = image.getWidth()/fidelity;
        int height = image.getHeight()/fidelity;
        final List<RGB> rgbs = new ArrayList<>(fidelity*fidelity);
        for (int x = 0; x < fidelity; x++){
            for (int y = 0; y < fidelity; y++){
                BufferedImage subimage = image.getSubimage(x*width,y*height,width,height);
                rgbs.add(getRGB(subimage));
            }
        }
        return new Sample(rgbs);
    }

    private RGB getRGB(BufferedImage image){
        int count = 0;

        int redTotal = 0;
        int blueTotal = 0;
        int greenTotal = 0;

        for (int h = 0; h < image.getHeight(); h++){
            for (int w = 0; w < image.getWidth(); w++){
                // Getting pixel color by position x and y
                int clr=  image.getRGB(w,h);
                int  red   = (clr & 0x00ff0000) >> 16;
                int  green = (clr & 0x0000ff00) >> 8;
                int  blue  =  clr & 0x000000ff;
                redTotal+=red;
                greenTotal+=green;
                blueTotal+=blue;

                count++;
            }
        }
        double blue = blueTotal/count;
        double red = redTotal/count;
        double green = greenTotal/count;
        return new RGB(red,green,blue);
    }
}
