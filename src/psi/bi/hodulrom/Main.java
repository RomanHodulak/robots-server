package psi.bi.hodulrom;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.Function;

class Vector2 {
	public int x;
	public int y;

	public Vector2(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Calculates Hamming distance to given vector.
	 *
	 * @param v Vector to calculate the distance to.
	 * @return Hamming distance as a non-negative integer.
	 */
	public int distance(Vector2 v) {
		return Math.abs(this.x - v.x) + Math.abs(this.y - v.y);
	}

	/**
	 * Calculates a directed angle between this and given vector.
	 *
	 * @param v Vector to calculate the angle to.
	 * @return Angle in degrees
	 */
	public double angle(Vector2 v) {
		return Math.atan2(this.y, this.x) - Math.atan2(v.y, v.x);
	}

	@Override
	public int hashCode() {
		return this.x * (3 << this.y);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Vector2) {
			Vector2 v = (Vector2) obj;

			return this.x == v.x && this.x == v.y;
		}

		return false;
	}
}

class Robot {
	/** Coordinates */
	private Vector2 pos = null;
	private Vector2 dir = null;

	/** Authorization */
	private final char usernameHash;
	private final char key;
	private boolean authorized = false;
	private boolean charging = false;

	/** Final areas explored for messages. */
	private Set<Vector2> discovered = new HashSet<>();

	public Robot(String username, char key) {
		this.usernameHash = this.hash(username);
		this.key = key;
	}

	public boolean authorize(char acceptCode) {
		this.authorized = (this.getUsernameHash() == (char) (acceptCode - this.key));

		return this.authorized;
	}

	public void moveTo(int x, int y) {
		if(this.knowsPosition()) {
			if(this.pos.x == x && this.pos.y == y) return;

			this.dir = new Vector2(x - this.pos.x, y - this.pos.y);
		}

		this.pos = new Vector2(x, y);
	}

	public boolean hasDiscovered(Vector2 v) {
		return discovered.contains(v);
	}

	/**
	 * Marks current robot position as discovered.
	 */
	public void discover() {
		this.discovered.add(this.pos);
	}

	public Vector2 getPosition() {
		return this.pos;
	}

	public Vector2 getDirection() {
		return this.dir;
	}

	public char getUsernameHash() {
		return this.usernameHash;
	}

	public boolean isAuthorized() {
		return this.authorized;
	}

	public void startCharging() {
		this.charging = true;
	}

	public void stopCharging() {
		this.charging = false;
	}

	public boolean isCharging() {
		return this.charging;
	}

	public boolean knowsPosition() {
		return this.pos != null;
	}

	public boolean knowsDirection() {
		return this.dir != null;
	}

	public boolean reachedTargetArea() {
		return this.pos != null && Math.abs(this.pos.x) == 2 && Math.abs(this.pos.y) == 2 && !this.hasDiscovered(this.pos);
	}

	private char hash(String username) {
		char hash = 0;

		for (int i = 0; i < username.length(); ++i) {
			hash += username.charAt(i) * 1000;
		}

		return hash;
	}

	public Vector2 rotateLeft() {
		if(this.dir.x == 1) {
			this.dir = new Vector2(0, 1);
		}
		else if(this.dir.x == -1) {
			this.dir =  new Vector2(0, -1);
		}
		else if(this.dir.y == 1) {
			this.dir = new Vector2(-1, 0);
		}
		else if(this.dir.y == -1) {
			this.dir = new Vector2(1, 0);
		}

		return this.dir;
	}

	public Vector2 rotateRight() {
		if(this.dir.x == 1) {
			this.dir = new Vector2(0, -1);
		}
		if(this.dir.x == -1) {
			this.dir = new Vector2(0, 1);
		}
		if(this.dir.y == 1) {
			this.dir = new Vector2(1, 0);
		}
		if(this.dir.y == -1) {
			this.dir = new Vector2(-1, 0);
		}

		return this.dir;
	}
}

class ServerResponse {
	/** Special sequence indicating end of message. */
	static final String TERMINATION_SEQUENCE = "\u0007\u0008"; // \a\b

	/** Map response code to string messages. */
	static Map<Integer, String> messages = new HashMap<>();

	/** Maps response to follow-up response, when multiple responses are sent. */
	static Map<Integer, Integer> followUps = new HashMap<>();

	static {
		messages.put(102, "MOVE");
		messages.put(103, "TURN LEFT");
		messages.put(104, "TURN RIGHT");
		messages.put(105, "GET MESSAGE");
		messages.put(106, "LOGOUT");
		messages.put(200, "OK");
		messages.put(300, "LOGIN FAILED");
		messages.put(301, "SYNTAX ERROR");
		messages.put(302, "LOGIC ERROR");
		followUps.put(200, 102);
	}

	/** Server response code. NULL means custom message. */
	private Integer code = null;

	/** Server response message. */
	private String message;

	public ServerResponse(int code) {
		this.code = code;
		this.message = ServerResponse.messages.get(code);
	}

	public ServerResponse(char acceptCode) {
		this.message = Integer.toString((int) acceptCode);
	}

	public boolean closesConnection() {
		return this.code != null && (this.code == 106 || this.code >= 300);
	}

	public ServerResponse followUp() {
		if(this.code != null && followUps.containsKey(this.code)) {
			return new ServerResponse(followUps.get(this.code));
		}

		return null;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder(4 + this.message.length() + TERMINATION_SEQUENCE.length());

		if(this.code != null) {
			builder.append(this.code.toString());
			builder.append(' ');
		}
		builder.append(this.message);
		builder.append(TERMINATION_SEQUENCE);

		return builder.toString();
	}

	public void print(PrintWriter out) {
		out.print(this.toString());
	}
}

enum Action {
	login(12),
	authorize(12),
	update(12),
	recharge(12),
	stopCharging(12),
	getMessage(100);

	private int maxLength;

	Action(int maxLength) {
		this.maxLength = maxLength;
	}

	public int getMaxLength() {
		return maxLength;
	}
}

class RobotServerException extends Exception {
	public final ServerResponse response;

	public RobotServerException(ServerResponse response) {
		super(response.toString());
		this.response = response;
	}
}

class RobotServer {
	private static final int TIMEOUT = 1000;
	private static final int TIMEOUT_CHARGING = 5000;
	private final char serverKey;
	private final char clientKey;
	private Robot robot = null;
	private Map<Action, Function<String, ServerResponse>> actions = new HashMap<>();

	/**
	 * Constructs new Robot Server instance.
	 *
	 * @param serverKey Server key.
	 * @param clientKey Robot key.
	 */
	public RobotServer(int serverKey, int clientKey) {
		this.serverKey = (char) serverKey;
		this.clientKey = (char) clientKey;

		// Initialize action callbacks
		actions.put(Action.login, this::login);
		actions.put(Action.authorize, this::authorize);
		actions.put(Action.update, this::update);
		actions.put(Action.stopCharging, this::stopCharging);
		actions.put(Action.getMessage, this::getMessage);
	}

	/**
	 * Starts listening on given port.
	 *
	 * @param port Port to listen on.
	 * @throws IOException Socket or stream could not be initialized (see exception message).
	 */
	public void listen(int port) throws IOException {
		ServerSocket serverSocket = new ServerSocket(port);
		Socket clientSocket = serverSocket.accept();
		System.out.println("client accepted from: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

		PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
		PrintWriter outc = new PrintWriter(System.out, true);
		BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		for (;;) {
			Action expectedAction = this.deduceAction();

			try {
				String request = this.getRequest(in, expectedAction);
				System.out.println("request:  " + request);
				clientSocket.setSoTimeout(TIMEOUT);

				if(this.requestsCharging(request)) {
					this.robot.startCharging();
					clientSocket.setSoTimeout(TIMEOUT_CHARGING);
					System.out.println("waiting for recharge");

					continue;
				}

				ServerResponse response = this.actions.get(expectedAction).apply(request);

				if(!this.handleResponse(response, out) && !this.handleResponse(response, outc)) break;

				out.flush();
				outc.flush();
			}
			catch (RobotServerException e) {
				this.handleResponse(e.response, out);
				this.handleResponse(e.response, outc);
				out.flush();
				outc.flush();
				break;
			}
			catch (SocketTimeoutException e) {
				break;
			}
		}

		System.out.println("connection on port " + port + " terminated.");
		out.close();
		in.close();
		clientSocket.close();
		serverSocket.close();
	}

	/**
	 * Properly handles the response and prints it out if needed.
	 *
	 * @param response Response to handle.
	 * @param out Output to write the responses to.
	 * @return TRUE if server can continue listening, FALSE if connection should be closed.
	 */
	private boolean handleResponse(ServerResponse response, PrintWriter out) {
		if(response == null) return true;

		response.print(out);

		if(response.closesConnection()) return false;

		for(ServerResponse followUp = response.followUp(); followUp != null; followUp = followUp.followUp()) {
			this.handleResponse(followUp, out);
		}

		return true;
	}

	/**
	 * Reads input stream until termination sequence or syntax error is detected.
	 *
	 * @param in Reader wrapping the input stream.
	 * @param expectedAction Action that is now expected to be run.
	 * @return Robot request.
	 * @throws IOException Error while reading from buffered reader.
	 * @throws RobotServerException Syntax error.
	 */
	private String getRequest(BufferedReader in, Action expectedAction) throws IOException, RobotServerException {
		StringBuilder builder = new StringBuilder(expectedAction.getMaxLength());

		while(!builder.toString().endsWith(ServerResponse.TERMINATION_SEQUENCE)) {
			if(builder.length() >= expectedAction.getMaxLength()) {
				throw new RobotServerException(new ServerResponse(301));
			}

			builder.append((char) in.read());
		}

		String buffer = builder.toString();

		return buffer.substring(0, buffer.length() - ServerResponse.TERMINATION_SEQUENCE.length());
	}

	/**
	 * Gets appropriate action for the robot to do next, based on its current state.
	 *
	 * @return Action to run.
	 */
	private Action deduceAction() {
		if(this.robot == null) {
			return Action.login;
		}
		if(this.robot.isCharging()) {
			return Action.stopCharging;
		}
		if(!this.robot.isAuthorized()) {
			return Action.authorize;
		}
		if(this.robot.reachedTargetArea()) {
			return Action.getMessage;
		}

		return Action.update;
	}

	/**
	 * Calculates accept code. Requires logged-in robot.
	 *
	 * @return Accept code calculated.
	 */
	private char acceptCode() {
		return (char) (this.robot.getUsernameHash() + this.serverKey);
	}

	/**
	 * Checks if the request requires charging.
	 *
	 * @param request Request
	 * @return TRUE if server should wait for robot to recharge.
	 */
	private boolean requestsCharging(String request) {
		return request.equals("RECHARGING");
	}

	/**
	 * Initializes robot with given username.
	 *
	 * @param request Request.
	 * @return Response.
	 */
	private ServerResponse login(String request) {
		this.robot = new Robot(request, this.clientKey);

		return new ServerResponse(this.acceptCode());
	}

	/**
	 * Attempts to authorize the robot.
	 *
	 * @param request Request.
	 * @return Response.
	 */
	private ServerResponse authorize(String request) {
		try {
			char clientAcceptCode = (char) Integer.parseUnsignedInt(request);

			if(this.robot.authorize(clientAcceptCode)) {
				return new ServerResponse(200);
			}

			return new ServerResponse(300);
		}
		catch (NumberFormatException e) {
			return new ServerResponse(301);
		}
	}

	/**
	 * Marks the robot as fully charged.
	 *
	 * @param request Request.
	 * @return Response.
	 */
	private ServerResponse stopCharging(String request) {
		if(!request.equals("FULL POWER")) {
			return new ServerResponse(302);
		}

		this.robot.stopCharging();

		return null;
	}

	/**
	 * Parses message pick up request. Robot will get logged out if a message has been found, otherwise keeps searching.
	 *
	 * @param request Request.
	 * @return Response.
	 */
	private ServerResponse getMessage(String request) {
		if(request.isEmpty()) {
			this.robot.discover();

			return this.update();
		}

		// Logout
		this.robot = null;

		return new ServerResponse(106);
	}

	/**
	 * Updates robot movement and direction.
	 *
	 * @param request Request.
	 * @return Response.
	 */
	private ServerResponse update(String request) {
		try {
			String[] parts = request.split(" ");

			if(parts.length < 3
				|| !parts[0].equals("OK")
				|| request.trim().length() < request.length() // trailing spaces
			) {
				return new ServerResponse(301);
			}

			int x = Integer.parseInt(parts[1]);
			int y = Integer.parseInt(parts[2]);

			this.robot.moveTo(x, y);

			return this.update();
		}
		catch (NumberFormatException e) {
			return new ServerResponse(301);
		}
	}

	/**
	 * Sets up directions for the robot based on its current position and direction.
	 *
	 * @return Response.
	 */
	private ServerResponse update() {
		if(!this.robot.knowsPosition()) {
			return new ServerResponse(102);
		}
		if(!this.robot.knowsDirection()) {
			return new ServerResponse(102);
		}

		Vector2 robotPos = this.robot.getPosition();
		Vector2 robotDir = this.robot.getDirection();
		Vector2 target = new Vector2(0, 0);
		int minDist = Integer.MAX_VALUE;

		for (int x = -2; x <= 2; x += 4) {
			for (int y = -2; y <= 2; y += 4) {
				Vector2 targetCandidate = new Vector2(x, y);

				if(this.robot.hasDiscovered(targetCandidate)) continue;

				int dist = robotPos.distance(targetCandidate);

				if (dist < minDist) {
					minDist = dist;
					target = targetCandidate;
				}
			}
		}

		// Has robot reached the target?
		if(robotPos.x == target.x && robotPos.y == target.y) {
			return new ServerResponse(105);
		}

		Vector2 targetDir = new Vector2(target.x - robotPos.x, target.y - robotPos.y);

		// Is robot set on a direction that brings him closer to target, when he moves?
		if(Math.signum(targetDir.x) == Math.signum(robotDir.x) || (Math.signum(targetDir.y) == Math.signum(robotDir.y))) {
			return new ServerResponse(102);
		}

		// Does rotating left help?
		Vector2 robotDirLeft = this.robot.rotateLeft();

		if(Math.signum(targetDir.x) == Math.signum(robotDirLeft.x) || Math.signum(targetDir.y) == Math.signum(robotDirLeft.y)) {
			return new ServerResponse(104);
		}

		this.robot.rotateRight();
		this.robot.rotateRight();

		// Rotating help does not help, just rotate right whatever the case.
		return new ServerResponse(103);
	}
}

public class Main {

	public static void main(String[] args) {
		RobotServer server = new RobotServer(54621, 45328);

		try {
			server.listen(2222);
		}
		catch (IOException ex) {
			System.out.println(ex.toString());
		}
	}
}
