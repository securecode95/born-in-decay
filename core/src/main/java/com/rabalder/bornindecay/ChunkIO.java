package com.rabalder.bornindecay;

import java.io.*;

public class ChunkIO {
    public static void saveChunk(Chunk chunk, String filename) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(chunk);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Chunk loadChunk(String filename) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            return (Chunk) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
