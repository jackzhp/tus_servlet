package org.tus.servlet.upload;

import javax.servlet.http.HttpServletRequest;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CONTINUE;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchHandler extends BaseHandler {

    private static final Logger log = LoggerFactory.getLogger(PatchHandler.class.getName());

    public PatchHandler(Composer composer, HttpServletRequest request, Response response) {
        super(composer, request, response);
    }

    @Override
    public void go() throws Exception {
        // Check content type header
        String ct = request.getHeader("Content-Type");
        if (ct == null || !ct.equals("application/offset+octet-stream")) {
            log.debug("Missing or invalid content type header.");
            throw new TusException.InvalidContentType();
        }

        // Check offset header
        Long offset = getLongHeader("upload-offset");
        if (offset == null || (long) offset < 0) {
            log.debug("upload-offset header missing or invalid.");
            throw new TusException.InvalidOffset();
        }

        // Get file ID from url
        String id = getID();
        if (id == null) {
            log.debug("No file id found in patch url");
            throw new TusException.NotFound();
        }

        boolean locked = false;
        try {
            locked = locker.lockUpload(id);
            if (!locked) {
                log.info("Couldn't lock " + id);
                throw new TusException.FileLocked();
            }
            whileLocked(id, offset);
        } finally {
            if (locked) {
                locker.unlockUpload(id);
            }
        }
    }

    private void whileLocked(String id, long offset)
            throws Exception {
        FileInfo fileInfo = datastore.getFileInfo(id);
        if (fileInfo == null) {
            log.debug("fileInfo not found for '" + id + "'");
            throw new TusException.NotFound();
        }
        // Offset in request header must match current file length.
        if (true) {
            if (offset != fileInfo.offset) {
                log.debug("current file size of " + fileInfo.offset + " doesn't match upload-offset of " + offset);
                response.setHeader("Upload-Offset", Long.toString(fileInfo.offset));
                response.setStatus(SC_BAD_REQUEST); //SC_NO_CONTENT
                return;//            throw new TusException.MismatchOffset();
            }
        } else if (offset < fileInfo.offset) {
            //this is allowed for the underlying call
        } else if (offset == fileInfo.offset) {
            //this is allowed for the underlying call
        } else { //offset > fileInfo.offset
            //this is not allowed for the underlying call.
        }

        long newOffset = fileInfo.offset;
        response.setHeader("Content-Length", "0");
        // Only write the data to store if we haven't already got the full file.
        if (fileInfo.offset == fileInfo.entityLength) {
            //we have received all, but still we are receiving more data.
            //TODO:
        }
        Long contentLength = getLongHeader("content-length");
        log.debug("Content-length is " + contentLength);

        // If contentLength header present, make sure contentLength + offset <= entityLength
        if (contentLength != null && ((long) contentLength + offset > fileInfo.entityLength)) {
            log.debug("content-length + offset > entity-length: " + contentLength + " + "
                    + offset + " > " + fileInfo.entityLength);
            throw new TusException.SizeExceeded();
        }

        // Don't exceed entityLength.
        long maxToRead = contentLength != null ? (long) contentLength : fileInfo.entityLength - offset;

        // Write the data.  
        long transferred = datastore.write(request, id, (long) offset, maxToRead);
        newOffset = transferred + (long) offset;

        // If upload is complete ...
        if (newOffset == fileInfo.entityLength) {
            //TODO: calculated sha256.
            int status=datastore.finish(fileInfo)?SC_OK:SC_CONFLICT;
            log.debug("Upload " + id + " is complete:"+status);
            response.setStatus(status);
            response.setHeader("Upload-Offset", Long.toString(newOffset));
            return;
        }
        //we do not have to set the newOffset to fileInfo. it will be loaded from disk.
        response.setHeader("Upload-Offset", Long.toString(newOffset));
        response.setStatus(SC_CONTINUE); //SC_NO_CONTENT
    }
}
