package CarRaceMVC;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import sun.security.jca.GetInstance;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ClientController {
	private Socket socket;
	private DataOutputStream dataOutput;
	private DataInputStream dataInput;
	private Thread serverThread;
	private ArrayList<View> windows;
	private static boolean waitForResponseLogin;
	private static boolean waitForResponseSignUp;
	private static boolean waitForResponseBet;
	private boolean login = false;
	private boolean signUp = false;
	private boolean bet = false;
	private String userName;
	private ArrayList<Car> raceCars;
	private String title;
	private int songNum;
	
	public ClientController(Stage stg) {
		Platform.setImplicitExit(false);
		try {
			socket = new Socket("localhost", 8000);
			dataInput = new DataInputStream(socket.getInputStream());
			dataOutput = new DataOutputStream(socket.getOutputStream());
			serverThread = new Thread(() -> run());
			serverThread.start();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		raceCars = new ArrayList<>();
		windows = new ArrayList<View>();
		start();
	}

	public void start() {
		System.out.println("START AT CLIENT");
		createNewWindow("Login");
	}

	public void run() {
		int i=0;
		while (true) {
			try {
				Thread.sleep(10);
				String data = dataInput.readUTF();
				parseData(data);
				Thread.sleep(10);
				System.out.println("I'm waiting" + i++);
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}
	}

	private void parseData(String data) {
		if ((data == null) || (data.isEmpty())) {
			System.out.println("Empty read or null");
			return;
		}
		System.out.println("Read Data From Server :: " + data);
		String[] args = data.split(",");

		switch (Integer.parseInt(args[0])) // args in the 0 place will send the
											// message type
		{
		case Defines.SIGNIN:
			setLogin(Boolean.parseBoolean(args[1]));
			break;
		case Defines.SIGNUP:
			setSignUp(Boolean.parseBoolean(args[1]));
			break;
		case Defines.BET:
			setBet(Boolean.parseBoolean(args[1]));
			break;
		case Defines.GET_RACE_DATA:
			readData(args);
			break;
		}
		// TODO This function will parse the message from the server and execute
		// the right actions
	}

	private void readData(String[] data) {
		raceCars.clear();
		songNum = Integer.parseInt(data[3]);
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date date = new Date();
		title = "Race #" + data[1] + " Created at " + dateFormat.format(date);
		for(int i = 0; i< 5; i++)
		{
			int id = Integer.parseInt(data[i*6+4]);
			int color = Integer.parseInt(data[i*6+5]);
			int speed = Integer.parseInt(data[i*6+6]);
			int model = Integer.parseInt(data[i*6+7]);
			int size = Integer.parseInt(data[i*6+8]);
			int manufacture = Integer.parseInt(data[i*6+9]);
			raceCars.add(new Car(id,model,null,Color.RED,size,speed,manufacture));
		}
		System.out.println("OpenWindow::In ClientController");
		
		Platform.runLater(() -> {
			createNewWindow("Race");
		});
	}

	private void setBet(boolean parseBoolean) {
		bet = parseBoolean;
		waitForResponseBet = false;
		
	}

	private void setSignUp(boolean parseBoolean) {
		signUp = parseBoolean;
		waitForResponseSignUp = false;
	}

	private void setLogin(boolean parseBoolean) {
		login = parseBoolean;
		waitForResponseLogin = false;
	}

	private void bet(String bet, String carName,View race) {
		try {
			dataOutput.writeUTF(Defines.BET + "," + userName + "," + bet + "," + carName);
		} catch (IOException ex) {
			System.err.println(ex);
		}
		
		waitForResponseBet = true;
		while (waitForResponseBet) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if(this.bet)
			MessageAlert("Bet placed successfuly","Good bet",race);
		else
			MessageAlert("Bet cannot be placed","Bad bet",race);
	}

	public void MessageAlert(String msg, String title, View parent) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.initOwner(parent);
		alert.setTitle(title);
		alert.setContentText(msg);
		alert.setOnCloseRequest(e -> {
			alert.close();
		});
		alert.show();
	}

	public void setOwnerStage(Stage stg) {
	}

	public void login(String user, String pass) {
		try {
			dataOutput.writeUTF(Defines.SIGNIN + "," + user + "," + pass);
		} catch (IOException e) {
			e.printStackTrace();
		}

		waitForResponseLogin = true;
		while (waitForResponseLogin) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (login) {
			windows.get(0).close();
			userName = user;
			openRaceWindows();
		} else
			MessageAlert("Invalid Username or Password, please try again",
					"Login Error", windows.get(0));
	}

	private void openRaceWindows() {
		try {
			dataOutput.writeUTF(Defines.GET_ALL_RACES_DATA + "");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
	/**
	 *This function sends the username and password to the server to be checked against the DB, waits for a response and Logs in if the information is correct
	 *or shows an error message if something went wrong
	 * @param user - username
	 * @param pass - password
	 * 
	 */
	public void signUp(String user, String pass) {
		try {
			dataOutput.writeUTF(Defines.SIGNUP + "," + user + "," + pass);
		} catch (IOException e) {
			e.printStackTrace();
		}
		waitForResponseSignUp = true;
		while (waitForResponseSignUp)
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		if (signUp)
			MessageAlert("Signed up succsesfully ! Way to go man!", "SignUp",
					windows.get(0));
		else
			MessageAlert("Username already used, please try again",
					"SignUp Error", windows.get(0));
	}

	public void createNewWindow(String type) {
		switch (type.toLowerCase()) {
		case "login":
			windows.add(new LoginView(this));
			break;
		case "race":

			windows.add(new RaceView(this,title,songNum,raceCars));
		}
		
		windows.get(windows.size() - 1).show();
		System.out.println(windows.get(windows.size() - 1).getTitle());

	}

	public void removeWindow(View view) {
		windows.remove(view);
	}
}
