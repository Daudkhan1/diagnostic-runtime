package app.api.diagnosticruntime.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.dcm4che3.imageio.plugins.dcm.DicomImageReadParam;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.Set;

@Slf4j
@Service
public class FileUtils {

    private static final Set<String> VALID_EXTENSIONS = Set.of(".svs", ".tif", ".tiff");


    public MultipartFile convertDicomToTiff(MultipartFile dicomFile) throws IOException {

        if (isTiffFile(dicomFile)) {
            return dicomFile; // Return the original file if it's already a TIFF
        }

        File tempTiffFile = null; // Temporary file reference
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        try (ImageInputStream input = ImageIO.createImageInputStream(dicomFile.getInputStream())) {
            // Get the DICOM ImageReader
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("DICOM");
            if (!readers.hasNext()) {
                throw new IOException("No DICOM ImageReader found.");
            }

            ImageReader reader = readers.next();
            reader.setInput(input);

            // Configure DICOM-specific parameters
            ImageReadParam param = new DicomImageReadParam();

            // Read the first frame (slice) of the DICOM image
            BufferedImage dicomImage = reader.read(0, param);

            // Write the BufferedImage to a temporary TIFF file
            String fileName = dicomFile.getOriginalFilename();
            String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;


            tempTiffFile = new File(tempDir, baseName + ".tiff");
            ImageIO.write(dicomImage, "TIFF", tempTiffFile);

            // Convert the TIFF file back to MultipartFile
            return convertFileToMultipartFile(tempTiffFile);
        }
    }


    /**
     * Converts a File to a MultipartFile.
     *
     * @param file The file to convert.
     * @return MultipartFile representation of the file.
     * @throws IOException If an error occurs during file reading.
     */
    private MultipartFile convertFileToMultipartFile(File file) throws IOException {
        return new MultipartFileAdapter(file);
    }

    /**
     * Checks if a MultipartFile is a TIFF file using Apache Tika.
     *
     * @param file The MultipartFile to check.
     * @return True if the file is a TIFF, false otherwise.
     * @throws IOException If an error occurs during reading.
     */
    private boolean isTiffFile(MultipartFile file) throws IOException {
        Tika tika = new Tika();
        String fileType = tika.detect(file.getInputStream());
        return fileType.equals("image/tiff") || fileType.equals("image/x.svs");
    }

    /**
     * Deletes all temporary files in the system's temp directory with the specified name.
     *
     * @param fileName The name of the file to delete.
     * @return true if all matching files were successfully deleted, false if any file could not be deleted.
     */
    public static boolean deleteTempFilesByName(String fileName) {
        // Get the system's temporary directory
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        boolean allDeleted = true;

        if (tempDir.exists() && tempDir.isDirectory()) {
            // List all files in the temp directory
            File[] tempFiles = tempDir.listFiles();
            if (tempFiles != null) {
                for (File file : tempFiles) {
                    // Check if the file matches the name
                    if (file.getName().equals(fileName)) {
                        // Try to delete the file
                        if (!file.delete()) {
                            log.error("Failed to delete temp file: " + file.getAbsolutePath());
                            allDeleted = false;
                        }
                    }
                }
            }
        } else {
            log.error("Temp directory does not exist or is not a directory.");
            return false;
        }

        return allDeleted;
    }

    /**
     * Custom implementation of MultipartFile to wrap a File.
     */
    private static class MultipartFileAdapter implements MultipartFile {

        private final File file;

        public MultipartFileAdapter(File file) {
            this.file = file;
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public String getOriginalFilename() {
            return file.getName();
        }

        @Override
        public String getContentType() {
            return "image/tiff";
        }

        @Override
        public boolean isEmpty() {
            return file.length() == 0;
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public byte[] getBytes() throws IOException {
            try (InputStream inputStream = new FileInputStream(file)) {
                return inputStream.readAllBytes();
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (InputStream in = new FileInputStream(file);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    public static String toTiffCompliantFileName(String fileName) {
        String extension = getExtension(fileName).toLowerCase();

        // 1. Validate
        if (!VALID_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Incompatible file type. File type must be TIFF or SVS.");
        }

        // 2. Convert .svs => .tif
        if (extension.equals(".svs")) {
            return removeExtension(fileName) + ".tif";
        }

        // 3. Otherwise, just return the original name
        return fileName;
    }

    /**
     * Extracts the portion of the file name from the final dot (.)
     * to the end. Example:
     *  - "slide.svs" -> ".svs"
     *  - "image.tiff" -> ".tiff"
     *  - "noExtension" -> "" (no dot found)
     */
    private static String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? "" : fileName.substring(dotIndex);
    }

    /**
     * Removes the file extension, if any. Example:
     *  - "slide.svs" -> "slide"
     *  - "folder/image.tiff" -> "folder/image"
     *  - "noExtension" -> "noExtension"
     */
    private static String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
    }
}
