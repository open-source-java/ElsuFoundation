package elsu.common;

import java.io.*;
import java.util.*;

/**
 *
 * @author Seraj.Dhaliwal
 */
class ExtensionFilter implements FilenameFilter {

    private String extension = null;

    public ExtensionFilter(String extension) {
        this.extension = extension;
    }

    @Override
    public boolean accept(File dir, String name) {
        return (name.matches(extension));
    }
}

public class FileUtils {

	public static String extractFilePath(String pAbsolutePath) {
		return pAbsolutePath.substring(0, pAbsolutePath.lastIndexOf(GlobalStack.FILESEPARATOR));
	}

	public static String extractFileName(String pAbsolutePath) {
		return pAbsolutePath.substring(pAbsolutePath.lastIndexOf(GlobalStack.FILESEPARATOR)+1);
	}
	
	public static int fileExists(String root, String mask) {
		int result = 0;
		
		ArrayList<String> als = FileUtils.findFiles(root, mask);
		for(String name : als) {
			result++;
		}
		
		return result;
	}
	
    public static ArrayList<String> findFiles(String root, String mask) {
        return findFiles(root, mask, false, true, 0);
    }

    public static ArrayList<String> findFiles(String root, String mask, boolean includeRootPath) {
        return findFiles(root, mask, false, includeRootPath, 0);
    }

    public static ArrayList<String> findFiles(String root, String mask, boolean recurse,
    		int maxFiles) {
        return findFiles(root, mask, recurse, true, maxFiles);
    }

    public static ArrayList<String> findFiles(String root, String mask,
            boolean recurse, boolean includeRootPath, final int maxFiles) {
        final ArrayList<String> result = new ArrayList<>();
        final String rootPath = root;
        final boolean includePath = includeRootPath; 

        // create filename filter
        final String fMask = "(?i)" + mask;
        final FilenameFilter fnFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if ((new File(dir.getPath() + GlobalStack.FILESEPARATOR + name)).isDirectory()) {
                    return true;
                } else {
                    return name.matches(fMask);
                }
            }
        };

        // local class to support recursive search
        // 20170407 - updated result.add to return fObject.getName() when path not required.
        class fileScanner {

            String lmask = "";
            boolean lrecurse = false;

            public fileScanner(String mask, boolean recurse) {
                lmask = mask;
                lrecurse = recurse;
            }

            public void scan(String directory, ArrayList<String> result) {
                File root = new File(directory);
                File[] fList = root.listFiles(fnFilter);

                for (File fObject : fList) {
                    String sFile = fObject.getAbsolutePath().toString();

                    if (fObject.isFile()) {
                        result.add((includePath) ? sFile : fObject.getName());

                        if ((maxFiles > 0) && (result.size() >= maxFiles)) {
                            break;
                        }
                    } else if (fObject.isDirectory()) {
                        if (lrecurse) {
                            scan(sFile, result);
                        }
                    }
                }
            }
        }

        (new fileScanner(mask.toLowerCase(), recurse)).scan(rootPath, result);
        return result;
    }

    public static void deleteFile(String file) {
    	File fObject;
    	
    	fObject = new File(file);
        fObject.delete();
    }

    public static void deleteFiles(String root, String mask) {
        deleteFiles(root, mask, false);
    }

    public static void deleteFiles(String root, String mask, boolean recurse) {
        ArrayList<String> fList = findFiles(root, mask, recurse, true, 0);

        for (String file : fList) {
            deleteFile(file);
        }
    }

    /**
     * readFile(...) method reads all the data from the text file. If
     * deleteOnExit option is set to true, the file is removed after reading
     * <p>
     * Two overloads are available: readFile(filename, deleteOnExit) method will
     * delete the file once all the content has been read into memory.
     *
     * readFile(filename, deleteOnExit, ignoreExceptions) method will delete the
     * file once all the content has been read into memory and will ignore any
     * exceptions in processing (so file will be deleted always)
     *
     * @param filename
     * @return <code>ArrayList</code> collection of file data
     * @throws java.lang.Exception
     */
    public static String readFile(String filename)
            throws Exception {
        return readFile(filename, false, false);
    }

    /**
     * readFile(...) method reads all the data from the text file and delete the
     * file once all the content has been read into memory.
     *
     * @param filename
     * @param deleteOnExit
     * @return <code>ArrayList</code> collection of file data
     * @throws java.lang.Exception
     */
    public static String readFile(String filename,
            boolean deleteOnExit)
            throws Exception {
        return readFile(filename, deleteOnExit, false);
    }

    /**
     * readFile(...) method reads all the data from the text file and delete the
     * file once all the content has been read into memory even if there are
     * exceptions in processing.
     *
     * @param filename
     * @param deleteOnExit
     * @param ignoreExceptions
     * @return <code>ArrayList</code> collection of file data
     * @throws java.lang.Exception
     */
    public static String readFile(String filename,
            boolean deleteOnExit,
            boolean ignoreExceptions)
            throws Exception {
        // return variable
        StringBuilder sb = new StringBuilder();

        // open the file to be read
        BufferedReader reader = null;

        // var to track if there was an exception in processing 
        boolean error = false;

        // if there is an exception in saving we need
        // to notify the client and exit.
        try {
            // open the output stream, reverse the overwrite since the file
            // writer is looking for append (true/false)
            reader = new BufferedReader(new FileReader(filename));

            // loop until the entire file has been read
            while (true) {
                // read the line from file
                String line = reader.readLine();

                // if line is null, then exit
                if (line == null) {
                    break;
                } else {
                    // store the line in buffer
                    sb.append(line);
                    sb.append(GlobalStack.LINESEPARATOR);
                }
            }
        } catch (Exception ex){
            // set error tracker to true
            error = true;

            // return the exception for notification
            if (!ignoreExceptions) {
                throw new Exception(ex);
            }
        } finally {
            // close the stream (if open), if not
            // ignore the exception
            try {
                reader.close();
            } catch (Exception exi){
            }

            // if deleteOnExit then try to delete based on the ignoreException
            // option (if true, then delete always, else no delete)
            if (deleteOnExit
                    && ((ignoreExceptions)
                    || (!ignoreExceptions && !error))) {
                try {
                    new File(filename.toString()).delete();
                } catch (Exception exi){
                }

            }
        }

        // return the data
        return sb.toString();
    }

    /**
     * readFile(...) method reads text file and returns the number of lines
     * requested from top.
     *
     * @param filename
     * @param lines
     * @return <code>ArrayList</code> collection of file dat
     * @throws java.lang.Exceptiona
     */
    public static String readFile(String filename, long lines)
            throws Exception {
        // return variable
        StringBuilder sb = new StringBuilder();

        // open the file to be read
        BufferedReader reader = null;

        // if there is an exception in saving we need
        // to notify the client and exit.
        try {
            // open the output stream, reverse the overwrite since the file
            // writer is looking for append (true/false)
            reader = new BufferedReader(new FileReader(filename));

            // loop until the file has been read to the lines required
            int count = 0;
            while (count < lines) {
                // read the line from file
                String line = reader.readLine();

                // if line is null, then exit
                if (line == null) {
                    break;
                } else {
                    // store the line in buffer
                    sb.append(line);
                    sb.append(GlobalStack.LINESEPARATOR);

                    // valid data, increase count
                    count++;
                }
            }
        } catch (Exception ex){
            // return the exception for notification
            throw new Exception(ex);
        } finally {
            // close the stream (if open), if not
            // ignore the exception
            try {
                reader.close();
            } catch (Exception exi){
            }
        }

        // return the data
        return sb.toString();
    }

    /**
     * readFile(...) method reads text file and returns the number of lines
     * requested from start position.
     *
     * @param filename
     * @param start
     * @param lines
     * @return <code>ArrayList</code> collection of file dat
     * @throws java.lang.Exceptiona
     */
    public static String readFile(String filename, long start,
            long lines)
            throws Exception {
        // return variable
        StringBuilder sb = new StringBuilder();

        // open the file to be read
        BufferedReader reader = null;

        // if there is an exception in saving we need
        // to notify the client and exit.
        try {
            // open the output stream, reverse the overwrite since the file
            // writer is looking for append (true/false)
            reader = new BufferedReader(new FileReader(filename));

            // loop until the file has been read to the lines required from
            // the start position specified
            int index = 0;
            int count = 0;
            while (count < lines) {
                // read the line from file
                String line = reader.readLine();

                // if line is null, then exit
                if (line == null) {
                    break;
                } else {
                    // check if the file is at the start position, if not,
                    // increase the index, and reloop
                    if (index < start) {
                        index++;
                        continue;
                    }

                    // store the line in buffer
                    sb.append(line);
                    sb.append(GlobalStack.LINESEPARATOR);

                    // valid data, increase count
                    count++;
                }
            }
        } catch (Exception ex){
            // return the exception for notification
            throw new Exception(ex);
        } finally {
            // close the stream (if open), if not
            // ignore the exception
            try {
                reader.close();
            } catch (Exception exi){
            }
        }

        // return the data
        return sb.toString();
    }

    /**
     * readFileToList(...) method reads all the data from the text file. If
     * deleteOnExit option is set to true, the file is removed after reading
     * <p>
     * Two overloads are available: readFile(filename, deleteOnExit) method will
     * delete the file once all the content has been read into memory.
     *
     * readFile(filename, deleteOnExit, ignoreExceptions) method will delete the
     * file once all the content has been read into memory and will ignore any
     * exceptions in processing (so file will be deleted always)
     *
     * @param filename
     * @return <code>ArrayList</code> collection of file data
     * @throws java.lang.Exception
     */
    public static ArrayList<String> readFileToList(String filename)
            throws Exception {
        return readFileToList(filename, false, false);
    }

    /**
     * readFileToList(...) method reads all the data from the text file and delete the
     * file once all the content has been read into memory.
     *
     * @param filename
     * @param deleteOnExit
     * @return <code>ArrayList</code> collection of file data
     * @throws java.lang.Exception
     */
    public static ArrayList<String> readFileToList(String filename,
            boolean deleteOnExit)
            throws Exception {
        return readFileToList(filename, deleteOnExit, false);
    }

    /**
     * readFileToList(...) method reads all the data from the text file and delete the
     * file once all the content has been read into memory even if there are
     * exceptions in processing.
     *
     * @param filename
     * @param deleteOnExit
     * @param ignoreExceptions
     * @return <code>ArrayList</code> collection of file data
     * @throws java.lang.Exception
     */
    public static ArrayList<String> readFileToList(String filename,
            boolean deleteOnExit,
            boolean ignoreExceptions)
            throws Exception {
        // return variable
        ArrayList<String> result = new ArrayList<>();

        // open the file to be read
        BufferedReader reader = null;

        // var to track if there was an exception in processing 
        boolean error = false;

        // if there is an exception in saving we need
        // to notify the client and exit.
        try {
            // open the output stream, reverse the overwrite since the file
            // writer is looking for append (true/false)
            reader = new BufferedReader(new FileReader(filename));

            // loop until the entire file has been read
            while (true) {
                // read the line from file
                String line = reader.readLine();

                // if line is null, then exit
                if (line == null) {
                    break;
                } else {
                    // store the line in buffer
                    result.add(line);
                }
            }
        } catch (Exception ex){
            // set error tracker to true
            error = true;

            // return the exception for notification
            if (!ignoreExceptions) {
                throw new Exception(ex);
            }
        } finally {
            // close the stream (if open), if not
            // ignore the exception
            try {
                reader.close();
            } catch (Exception exi){
            }

            // if deleteOnExit then try to delete based on the ignoreException
            // option (if true, then delete always, else no delete)
            if (deleteOnExit
                    && ((ignoreExceptions)
                    || (!ignoreExceptions && !error))) {
                try {
                    new File(filename.toString()).delete();
                } catch (Exception exi){
                }

            }
        }

        // return the data
        return result;
    }

    /**
     * readFileToList(...) method reads text file and returns the number of lines
     * requested from top.
     *
     * @param filename
     * @param lines
     * @return <code>ArrayList</code> collection of file dat
     * @throws java.lang.Exceptiona
     */
    public static ArrayList<String> readFileToList(String filename, long lines)
            throws Exception {
        // return variable
        ArrayList<String> result = new ArrayList<>();

        // open the file to be read
        BufferedReader reader = null;

        // if there is an exception in saving we need
        // to notify the client and exit.
        try {
            // open the output stream, reverse the overwrite since the file
            // writer is looking for append (true/false)
            reader = new BufferedReader(new FileReader(filename));

            // loop until the file has been read to the lines required
            int count = 0;
            while (count < lines) {
                // read the line from file
                String line = reader.readLine();

                // if line is null, then exit
                if (line == null) {
                    break;
                } else {
                    // store the line in buffer
                    result.add(line);

                    // valid data, increase count
                    count++;
                }
            }
        } catch (Exception ex){
            // return the exception for notification
            throw new Exception(ex);
        } finally {
            // close the stream (if open), if not
            // ignore the exception
            try {
                reader.close();
            } catch (Exception exi){
            }
        }

        // return the data
        return result;
    }

    /**
     * readFileToList(...) method reads text file and returns the number of lines
     * requested from start position.
     *
     * @param filename
     * @param start
     * @param lines
     * @return <code>ArrayList</code> collection of file dat
     * @throws java.lang.Exceptiona
     */
    public static ArrayList<String> readFileToList(String filename, long start,
            long lines)
            throws Exception {
        // return variable
        ArrayList<String> result = new ArrayList<>();

        // open the file to be read
        BufferedReader reader = null;

        // if there is an exception in saving we need
        // to notify the client and exit.
        try {
            // open the output stream, reverse the overwrite since the file
            // writer is looking for append (true/false)
            reader = new BufferedReader(new FileReader(filename));

            // loop until the file has been read to the lines required from
            // the start position specified
            int index = 0;
            int count = 0;
            while (count < lines) {
                // read the line from file
                String line = reader.readLine();

                // if line is null, then exit
                if (line == null) {
                    break;
                } else {
                    // check if the file is at the start position, if not,
                    // increase the index, and reloop
                    if (index < start) {
                        index++;
                        continue;
                    }

                    // store the line in buffer
                    result.add(line);

                    // valid data, increase count
                    count++;
                }
            }
        } catch (Exception ex){
            // return the exception for notification
            throw new Exception(ex);
        } finally {
            // close the stream (if open), if not
            // ignore the exception
            try {
                reader.close();
            } catch (Exception exi){
            }
        }

        // return the data
        return result;
    }

    /**
     * tempFile(...) method returns a string formatted with random chars to
     * create temp file for processing.
     *
     * @param length
     * @param extension
     * @return <code>String</code> temp file name
     */
    public static String tempFile(int length, String extension) {
        return StringUtils.randomString(length)
                + (extension.contains(".") ? extension : "." + extension);
    }

    /**
     * tempFile(...) method returns a string formatted with random chars to
     * create temp file for processing.
     *
     * @param length
     * @param prefix
     * @param suffix
     * @param extension
     * @return <code>String</code> temp file name
     */
    public static String tempFile(int length, String prefix, String suffix, String extension) {
        String name = prefix + StringUtils.randomString(length) + suffix;
        return name
                + (extension.contains(".") ? extension : "." + extension);
    }

    /**
     * writeFile(...) method writes the data provided to the filename (which
     * includes path info). Overwrite options specifies if the existing file
     * data is kept or cleared.
     *
     * @param filename
     * @param data
     * @param overwrite
     * @throws Exception
     */
    public static void writeFile(String filename, String data, boolean overwrite)
            throws Exception {
        // storage for the file stream for saving the message
        FileWriter writer = null;

        // if there is an exception in saving we need
        // to notify the client and exit.
        try {
            // open the output stream, reverse the overwrite since the file
            // writer is looking for append (true/false)
            writer = new FileWriter(filename, !overwrite);

            // write the message with terminator
            writer.write(data);

            // flush the text file to disk
            writer.flush();
        } catch (Exception ex){
            // return the exception for notification
            throw new Exception(ex);
        } finally {
            // close the stream (if open), if not
            // ignore the exception
            try {
                writer.close();
            } catch (Exception exi){
            }
        }
    }
    
    // 20170127 - added to save list to file
    // 20200815 - updated to save object to file
    public static void writeFile(String filename, List<Object> data, boolean overwrite)
            throws Exception {
        // storage for the file stream for saving the message
        FileWriter writer = null;

        // if there is an exception in saving we need
        // to notify the client and exit.
        try {
            // open the output stream, reverse the overwrite since the file
            // writer is looking for append (true/false)
            writer = new FileWriter(filename, !overwrite);

            // write the message with terminator
            for(Object line : data) {
            	writer.write(line + GlobalStack.LINESEPARATOR);
            }

            // flush the text file to disk
            writer.flush();
        } catch (Exception ex){
        	System.out.println("elsu.FileUtils, " + "writeFile(), " + ex.getMessage());
            // return the exception for notification
            throw new Exception(ex);
        } finally {
            // close the stream (if open), if not
            // ignore the exception
            try {
                writer.close();
            } catch (Exception exi){
            }
        }
    }
}
