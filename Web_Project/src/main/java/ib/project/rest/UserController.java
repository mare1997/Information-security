package ib.project.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;
import javax.websocket.server.PathParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ib.project.dto.UserDTO;
import ib.project.model.User;
import ib.project.service.UserService;




@RestController
@RequestMapping(value = "/api/demo")
@CrossOrigin("*")
public class DemoController {

	private static String DATA_DIR_PATH;

	@Autowired
	ServletContext context;

	@Autowired
	UserService userService;
	
	static {
		ResourceBundle rb = ResourceBundle.getBundle("application");
		DATA_DIR_PATH = rb.getString("dataDir");
	}

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<String> createAFileInResources() throws IOException {

		byte[] content = "Content".getBytes();
		
		String directoryPath = getResourceFilePath(DATA_DIR_PATH).getAbsolutePath();
 		
		Path path = Paths.get(directoryPath + File.separator + "demo.txt");

		Files.write(path, content);
		return new ResponseEntity<String>(path.toString(), HttpStatus.OK);
	}
	
	
	
	@RequestMapping(value = "/download/jks/{name}", method = RequestMethod.POST)
	public ResponseEntity<byte[]> downloadJks (@PathVariable("name") String name) {
		System.out.println("USao u download");
		System.out.println(name);
		User user=userService.getByEmail(name);
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir+"\\data\\"+user.getEmail()+".jks");
		File file = new File(workingDir+"\\data\\"+user.getEmail()+".jks");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("filename",user.getEmail()+".jks");
		System.out.println(headers.get("filename"));
		byte[] bFile = readBytesFromFile(file.toString());

		return ResponseEntity.ok().headers(headers).body(bFile);
	}
	@RequestMapping(value = "/download/cer/{name}", method = RequestMethod.POST)
	public ResponseEntity<byte[]> downloadCer (@PathVariable("name") String name) {
		System.out.println("USao u download");
		System.out.println(name);
		User user=userService.getByEmail(name);
		String workingDir = System.getProperty("user.dir");
		System.out.println("Current working directory : " + workingDir+"\\data\\"+user.getEmail()+".cer");
		File file = new File(workingDir+"\\data\\"+user.getEmail()+".cer");
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("filename",user.getEmail()+".cer");
		System.out.println(headers.get("filename"));
		byte[] bFile= null;
		bFile = readBytesFromFile(file.toString());
			
		return ResponseEntity.ok().headers(headers).body(bFile);
	}
	
	
	
	
	
	
	private static byte[] readBytesFromFile(String filePath) {

		FileInputStream fileInputStream = null;
		byte[] bytesArray = null;
		try {

			File file = new File(filePath);
			bytesArray = new byte[(int) file.length()];

			// read file into bytes[]
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bytesArray);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		return bytesArray;

	}

	public File getResourceFilePath(String path) {
		
		URL url = this.getClass().getClassLoader().getResource(path);
		File file = null;

		try {
			
			file = new File(url.toURI());
		} catch (Exception e) {
			file = new File(url.getPath());
		}

		return file;
	}
}
