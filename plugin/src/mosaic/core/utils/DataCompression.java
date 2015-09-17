package mosaic.core.utils;


import java.io.File;
import java.io.IOException;
import java.util.Vector;

import mosaic.core.cluster.ShellProcessOutput;


/**
 * This Class is an helping class to compress and uncompress files and directory
 * it support tar zip 7zip rar
 *
 * @author Pietro Incardona
 */

public class DataCompression {

    int selC = -1;

    public class Algorithm {

        private Algorithm(String name_, String cmd_, String finger_print_, String cc, String uc) {
            name = name_;
            cmd = cmd_;
            finger_print = finger_print_;
            compress_command = cc;
            uncompress_command = uc;
        }

        public String name;
        String cmd;
        String finger_print;
        String compress_command;
        String uncompress_command;
    }

    Vector<Algorithm> al;

    public DataCompression() {
        al = new Vector<Algorithm>();

        al.add(new Algorithm("SZIP", "7za --help", "7-Zip", "7za a -t7z # *", "yes | 7za e *"));
        al.add(new Algorithm("TAR", "tar --version", "tar (GNU tar)", "tar -j -cvf # * ", "tar -xvf * "));
        al.add(new Algorithm("ZIP", "zip --version", "This is Zip", "zip # * ; mv #.zip #", "unzip  *"));
    }

    /**
     * Select internaly the best compressor
     */

    public void selectCompressor() {
        for (int i = 0; i < al.size(); i++) {
            String out = null;
            try {
                out = ShellCommand.exeCmdString(al.get(i).cmd);
            }
            catch (IOException e) {
                e.printStackTrace();
                out = new String();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                out = new String();
            }

            if (out != null && out.contains(al.get(i).finger_print)) {
                selC = i;
                return;
            }
        }
    }

    /**
     * Select internaly the best compressor starting from the j compressor
     *
     * @param j
     */

    // private void selectCompressor(int j)
    // {
    // int i = 0;
    // for (i = j+1 ; i < al.size() ; i++)
    // {
    // String out = null;
    // try {
    // out = ShellCommand.exeCmdString(al.get(i).cmd);
    // } catch (IOException e) {
    // e.printStackTrace();
    // return;
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // return;
    // }
    //
    // if (out.contains(al.get(i).finger_print))
    // {
    // selC = i;
    // return;
    // }
    // }
    //
    // selC = i;
    // }

    /**
     * Select internaly the next compressor
     */

    public boolean nextCompressor() {
        if (selC >= al.size()) {
            return false;
        }
        selC++;
        return true;
    }

    /**
     * Return the algorithm selected
     *
     * @return the algorithm selected null there os no selection
     */

    public Algorithm getCompressor() {
        if (selC == -1 || selC >= al.size()) {
            return null;
        }

        return al.get(selC);
    }

    /**
     * It compress a set of files and directory
     *
     * @param start_dir starting dir
     * @param fs Array of files,relative from starting dir
     * @param file_a Archive
     * @param ShellProcessOutput interface to process the output
     * @return true if archive is created
     */

    public boolean Compress(File start_dir, File[] fs, File file_a, ShellProcessOutput out) {
        if (selC == -1) {
            return false;
        }

        String o = new String();

        for (int i = 0; i < fs.length; i++) {
            o += fs[i].getPath() + " ";
        }

        try {
            ShellCommand.exeCmd(al.get(selC).compress_command.replace("#", file_a.getAbsolutePath()).replace("*", o) + " ; echo \"COMPRESSION END\"; \n", start_dir, null, out);
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Uncompress archive
     *
     * @param file_a Archive to uncompress
     * @param work_dir Working directory
     * @return true if the archive has been uncompressed
     */

    public boolean unCompress(File file_a, File work_dir) {
        try {
            ShellCommand.exeCmd(al.get(selC).uncompress_command.replace("*", file_a.getAbsolutePath()), work_dir, null);
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Get uncompress archive command
     *
     * @param file_a Archive to uncompress
     * @return Command string to uncompress the archive,
     *         null if no-compression algorithm has been selected
     */

    public String unCompressCommand(File file_a) {
        if (selC == -1) {
            return null;
        }

        return al.get(selC).uncompress_command.replace("*", file_a.getAbsolutePath());
    }

    /**
     * Get compress archive command
     *
     * @param start_dir starting dir
     * @param fs Array of files, relative path from starting dir
     * @param file_a Archive
     * @return Command string to compress the archive
     */

    public String compressCommand(File start_dir, File[] fs, File file_a) {
        if (selC == -1) {
            return null;
        }

        String o = new String();

        for (int i = 0; i < fs.length; i++) {
            o += fs[i].getPath() + " ";
        }

        return al.get(selC).compress_command.replace("#", file_a.getAbsolutePath()).replace("*", o);
    }

    /**
     * Get a list of all compressor algorithm
     *
     * @return A list of all compression algorithms
     */

    public Vector<Algorithm> getCompressorList() {
        return al;
    }
}
