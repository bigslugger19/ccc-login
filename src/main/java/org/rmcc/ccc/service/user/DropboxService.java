package org.rmcc.ccc.service.user;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxRequestUtil;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.dropbox.core.v2.users.FullAccount;

import org.rmcc.ccc.model.CccMetadata;
import org.rmcc.ccc.model.DropboxFile;
import org.rmcc.ccc.model.DropboxFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@Service
public class DropboxService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DropboxService.class);
	
//	@Value("${ccc.dropbox.accessToken}")
//    private String accessToken;
	static final String ACCESS_TOKEN = "8pUCurIPXlUAAAAAAAAIGeOLExI8YLErAizpTMiV5EhbWhDw5jA83Yw3RKkjCz0D"; //mine
//	static final String ACCESS_TOKEN = "0zSFsnWoJOUAAAAAAAEafHy4-QOqabrvGxliLU3rkk1XAu0GkpdWfLwzciXM_f6B"; //catconservancy
	
    private DbxRequestConfig config;
    private DbxClientV2 client;
    private FullAccount account;
	
	public DropboxService() throws DbxException, IOException {
		// Create Dropbox client
		config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
		client = new DbxClientV2(config, ACCESS_TOKEN);

        // Get current account info
        try {
            account = client.users.getCurrentAccount();
        } catch (DbxException e) {
            LOGGER.error("Can't connect to dropbox.");
        }

//        // Upload "test.txt" to Dropbox
//        InputStream in = new FileInputStream("test.txt");
//        DbxFiles.FileMetadata metadata = client.files.uploadBuilder("/test.txt").run(in);
	}
	
	public List<DropboxFolder> getFoldersByPath(String path) throws DbxException, IOException {
		
		List<DropboxFolder> folders = new ArrayList<DropboxFolder>();
		
		for (Metadata metadata : getFolderContentsByPath(path)) {
			CccMetadata m = (CccMetadata) metadata;
			if (m.isDir()) {
				DropboxFolder f = new DropboxFolder();
				f.setFiles(getFilesByPath(m.getPathLower()));
				f.setFolders(getFoldersByPath(m.getPathLower()));
				LOGGER.info("files", f.getFiles());
				LOGGER.info("folders", f.getFolders());
				folders.add(f);
			}
		}
		return folders;
	}
	
	private List<DropboxFile> getFilesByPath(String path) throws DbxException, IOException {
		
		List<DropboxFile> files = new ArrayList<DropboxFile>();
		
		for (Metadata metadata : getFolderContentsByPath(path)) {
			CccMetadata m = (CccMetadata) metadata;
			if (!m.isDir()) {
				DropboxFile f = new DropboxFile();
				f.setName(m.getName());
				f.setPath(m.getPathLower());
				files.add(f);
			}
		}
		return files;
	}

	public List<Metadata> getFolderContentsByPath(String path) throws DbxException, IOException {
		
		List<Metadata> encodedPaths = new ArrayList<Metadata>();
		
		 // Get the folder listing from Dropbox.
        TreeMap<String,Metadata> children = new TreeMap<String,Metadata>();

        ListFolderResult result;
        try {
            try {
                result = client.files.listFolder(path);
            } catch (ListFolderErrorException ex) {
                if (ex.errorValue.isPath()) {
                	LOGGER.error("Dropbox error", ex);
                    if (checkPathError(path, ex.errorValue.getPathValue())) return null;
                }
                throw ex;
            }

            while (true) {
                for (Metadata md : result.getEntries()) {
                    if (md instanceof DeletedMetadata) {
                        children.remove(md.getPathLower());
                    } else {
                        children.put(md.getPathLower(), md);
                    }
                }

                if (!result.getHasMore()) break;

                try {
                    result = client.files.listFolderContinue(result.getCursor());
                }
                catch (ListFolderContinueErrorException ex) {
                    if (ex.errorValue.isPath()) {
                    	LOGGER.error("Dropbox error", ex);
                        if (checkPathError(path, ex.errorValue.getPathValue())) break;
                    }
                    throw ex;
                }
            }
        }
        catch (DbxException ex) {
        	LOGGER.error("Dropbox error", ex);
//            common.handleDbxException(response, user, ex, "listFolder(" + jq(path) + ")");
            throw ex;
        }
        
        for (Metadata child : children.values()) {
        	boolean isDir = (child instanceof FolderMetadata);
    		CccMetadata cccMetadata = new CccMetadata(child, isDir);
			encodedPaths.add(cccMetadata);
        }
        
		return encodedPaths;
	}
	
	public InputStream getInputStreamByPath(String path) throws DownloadErrorException, DbxException, IOException {
		return client.files.download(path).getInputStream();
	}
	
	public InputStream getThumbnailInputStreamByPath(String path) throws DownloadErrorException, DbxException, IOException {
		return client.files.getThumbnail(path).getInputStream();
	}

    private boolean checkPathError(String path, LookupError le) {
        switch (le.tag()) {
            case NOT_FOUND:
            case NOT_FOLDER:
            	LOGGER.error("Path doesn't exist on Dropbox:");
                return true;
        }
        return false;
    }


}
