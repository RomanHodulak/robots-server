package psi.bi.hodulrom;

import java.io.*;
import java.net.*;

class Vector2 {

	private int x;
	private int y;

	public Vector2(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Vector2() {
		this.x = 0;
		this.y = 0;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public void setTo(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Multiplies all components by given coefficient.
	 *
	 * @param m Coefficient to multiply with.
	 */
	public void multiply(int m) {
		this.x *= m;
		this.y *= m;
	}

	/**
	 * Adds given vector to this vector.
	 *
	 * @param vec Vector to add to this vector.
	 */
	public void add(Vector2 vec) {
		this.x += vec.x;
		this.y += vec.y;
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

	@Override
	public String toString() {
		return "[" + this.x + ", " + this.y + "]";
	}

	@Override
	public Vector2 clone() {
		return new Vector2(this.x, this.y);
	}

	@Override
	public int hashCode() {
		return (33 ^ this.x) * (33 ^ this.y); // djb2-like hash
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Vector2)) return false;
		if (obj == this) return true;

		Vector2 v = (Vector2) obj;

		return this.x == v.x && this.x == v.y;
	}
}

enum Direction {
	turnLeft,
	turnRight,
	moveForward
}

class Robot {

	/** Robot position (if known). */
	private Vector2 pos = null;

	/** Robot direction (if known). */
	private Vector2 dir = null;

	/** Charge status */
	private boolean charging = false;

	/** Fields of the target area. TRUE if searched for message, FALSE if not. Coordinates are indices shifted by +2. */
	private boolean searched[][] = new boolean[5][5];

	public Robot() {
		for (int i = 0; i < searched.length; ++i) {
			for (int j = 0; j < searched[i].length; ++j) {
				this.searched[i][j] = false;
			}
		}
	}

	/**
	 * Checks charging status.
	 *
	 * @return TRUE if robot is charging, FALSE otherwise.
	 */
	public boolean isCharging() {
		return this.charging;
	}

	/**
	 * Puts the robot in a charging state.
	 */
	public void startCharging() {
		this.charging = true;
	}

	/**
	 * Removes charging state.
	 */
	public void stopCharging() {
		this.charging = false;
	}

	/**
	 * Checks if the current position of the robot is known.
	 */
	public boolean knowsPosition() {
		return this.pos != null;
	}

	/**
	 * Checks if the current direction of the robot is known.
	 */
	public boolean knowsDirection() {
		return this.dir != null;
	}

	/**
	 * Marks current robot position as searched.
	 */
	public void search() {
		if (this.isSearchable(this.pos)) {
			this.searched[this.pos.getX() + 2][this.pos.getY() + 2] = true;
		}
	}

	/**
	 * Sets the robot position, determining its direction as well.
	 *
	 * @param x X position coordinate
	 * @param y Y position coordinate
	 */
	public void moveTo(int x, int y) {
		if (!this.knowsPosition()) {
			this.pos = new Vector2(x, y);

			return;
		}

		if (this.pos.getX() == x && this.pos.getY() == y) return;

		int dirX = x - this.pos.getX();
		int dirY = y - this.pos.getY();

		if (Math.abs(dirX) > 1 && Math.abs(dirY) > 1) {
			// Some weird jump happened, robot can only move by one at a time. Mark the direction as unknown.
			this.dir = null;
		}
		else {
			if (this.dir == null) {
				this.dir = new Vector2();
			}

			this.dir.setTo(dirX, dirY);
		}

		this.pos.setTo(x, y);
	}

	/**
	 * Rotates direction -90 degrees.
	 */
	public void turnLeft() {
		this.dir = this.turnLeft(this.dir);
	}

	/**
	 * Rotates direction +90 degrees.
	 */
	public void turnRight() {
		this.dir = this.turnRight(this.dir);
	}

	/**
	 * Calculates the shortest direction to the nearest unsearched field within target area.
	 *
	 * @return Direction to take next.
	 */
	public Direction calculateDirection() {
		if (!this.knowsPosition()) {
			return Direction.moveForward;
		}

		if (!this.knowsDirection()) {
			return Direction.moveForward;
		}

		Vector2 nearestTarget = this.findNearestSearchable();

		Vector2 dirLeft = this.turnLeft(this.dir);
		Vector2 dirRight = this.turnRight(this.dir);

		Vector2 posNext = this.pos.clone();
		Vector2 posNextLeft = this.pos.clone();
		Vector2 posNextRight = this.pos.clone();

		posNext.add(this.dir);
		posNextLeft.add(dirLeft);
		posNextLeft.add(dirRight);

		int distNext = posNext.distance(nearestTarget);
		int distNextLeft = posNextLeft.distance(nearestTarget);
		int distNextRight = posNextRight.distance(nearestTarget);

		if (distNext <= distNextLeft && distNext <= distNextRight) {
			return Direction.moveForward;
		}

		if (distNextRight <= distNext && distNextRight <= distNextLeft) {
			return Direction.turnRight;
		}

		return Direction.turnLeft;
	}

	/**
	 * Finds nearest unsearched target field. Operates by brute-force but it takes only 25 steps.
	 *
	 * @return Nearest unsearched field within target area.
	 */
	private Vector2 findNearestSearchable() {
		Vector2 target = null;
		Vector2 targetCandidate = new Vector2();
		int minDist = Integer.MAX_VALUE;

		for (int x = -2; x <= 2; ++x) {
			for (int y = -2; y <= 2; ++y) {
				targetCandidate.setTo(x, y);

				if (this.isSearched(targetCandidate)) continue;

				int dist = this.pos.distance(targetCandidate);

				if (dist < minDist) {
					minDist = dist;

					if (target == null) {
						target = new Vector2();
					}

					target.setTo(targetCandidate.getX(), targetCandidate.getY());
				}
			}
		}

		return target;
	}

	/**
	 * Checks if the robot is positioned within target area and on unsearched field.
	 */
	public boolean standsOnSearchable() {
		return this.knowsPosition() && this.isSearchable(this.pos);
	}

	/**
	 * Turns left or right if direction is known.
	 *
	 * @param direction Direction to apply.
	 */
	public void tryRotate(Direction direction) {
		if (!this.knowsDirection()) return;

		switch (direction) {
			case turnLeft:
				this.turnLeft();
				break;
			case turnRight:
				this.turnRight();
				break;
		}
	}

	/**
	 * Returns copy of the given directional vector, rotated by +90 degrees.
	 *
	 * @return New direction.
	 */
	private Vector2 turnRight(Vector2 vec) {
		if (vec.getY() == 0) {
			return new Vector2(0, -this.dir.getX());
		}

		return new Vector2(this.dir.getY(), 0);
	}

	/**
	 * Returns copy of the given directional vector, rotated by -90 degrees.
	 *
	 * @return New direction.
	 */
	private Vector2 turnLeft(Vector2 vec) {
		if (vec.getY() == 0) {
			return new Vector2(0, this.dir.getX());
		}

		return new Vector2(-this.dir.getY(), 0);
	}

	/**
	 * Checks if given positional vector is within target area and searched for messages.
	 *
	 * @param vec Position to check.
	 */
	private boolean isSearched(Vector2 vec) {
		return this.isWithinTarget(vec) && this.searched[vec.getX() + 2][vec.getY() + 2];
	}

	/**
	 * Checks if given positional vector is within target area and on unsearched field.
	 *
	 * @param vec Position to check.
	 */
	private boolean isSearchable(Vector2 vec) {
		return this.isWithinTarget(vec) && !this.isSearched(vec);
	}

	/**
	 * Checks if given positional vector is within target area.
	 *
	 * @param vec Position to check.
	 */
	private boolean isWithinTarget(Vector2 vec) {
		return Math.abs(vec.getX()) <= 2 && Math.abs(vec.getY()) <= 2;
	}
}

abstract class RobotException extends Exception {

	/**
	 * Informs the client about the error.
	 *
	 * @param client Client to report the exception to.
	 */
	public abstract void reportTo(Client client);
}

class SyntaxErrorException extends RobotException {
	@Override
	public void reportTo(Client client) {
		client.sendMessage(ServerMessage.syntaxError);
	}
}

class LogicErrorException extends RobotException {
	@Override
	public void reportTo(Client client) {
		client.sendMessage(ServerMessage.logicError);
	}
}

class LoginFailedException extends RobotException {
	@Override
	public void reportTo(Client client) {
		client.sendMessage(ServerMessage.loginFailed);
	}
}

class MessageValidator {

	/** Regular expression to check the message against. */
	public final String regex;

	/** Maximum allowed length of the message. */
	public final int maxLength;

	public MessageValidator(String regex, int maxLength) {
		this.regex = regex;
		this.maxLength = maxLength;
	}

	/**
	 * Checks if the message is considered complete.
	 *
	 * @param message Message to check.
	 * @return TRUE if the message is complete and can be processed, FALSE otherwise.
	 */
	public boolean isComplete(String message) {
		return message.endsWith(ServerMessage.TERMINATION_SEQUENCE) || message.length() >= this.maxLength;
	}

	/**
	 * Checks if the message is considered syntactically valid. Only complete messages should go through this check.
	 *
	 * @param message Message to validate.
	 * @return TRUE if message is valid, FALSE if message is invalid and should not be processed.
	 */
	public boolean check(String message) {
		if (!message.endsWith(ServerMessage.TERMINATION_SEQUENCE)) return false;
		if (message.length() > this.maxLength) return false;

		// Message without the termination sequence
		String messageOnly = message.substring(0, message.length() - ServerMessage.TERMINATION_SEQUENCE.length());

		return messageOnly.matches(this.regex);
	}
}

abstract class RobotMessage {

	/**
	 * Handles the client according to the message.
	 *
	 * @param client Client to update.
	 * @return Validator for the next message from the client.
	 * @throws RobotException Invalid message and/or client state.
	 */
	public abstract MessageValidator handle(Client client) throws RobotException;

	/**
	 * Regular update that sends directions to the client to keep searching.
	 *
	 * @param client Client.
	 * @return Validator of the next message from the client.
	 */
	protected MessageValidator searchUpdate(Client client) {
		if (client.robot.standsOnSearchable()) {
			client.sendMessage(ServerMessage.pickUp);

			// Expected message or empty string
			return new MessageValidator(".*", 100);
		}

		Direction direction = client.robot.calculateDirection();
		client.robot.tryRotate(direction);

		client.sendMessage(ServerMessage.fromDirection(direction));

		// Expected position update or recharge
		return new MessageValidator("^(" + RobotOkMessage.REQUEST_REGEX + "|RECHARGING)$", 12);
	}
}

class RobotUsernameMessage extends RobotMessage {

	/** Username obtained from the request. */
	private final String username;

	public RobotUsernameMessage(String request) {
		this.username = request;
	}

	@Override
	public MessageValidator handle(Client client) {
		client.login(this.username);
		client.sendMessage(ServerMessage.confirmation(client.serverAcceptCode()));

		// Expected accept code or recharge
		return new MessageValidator("^([0-9]{1,5}|RECHARGING)$", 12);
	}
}

class RobotRechargingMessage extends RobotMessage {

	public RobotRechargingMessage(String request) throws SyntaxErrorException {
		if (!request.matches("RECHARGING")) {
			throw new SyntaxErrorException();
		}
	}

	@Override
	public MessageValidator handle(Client client) {
		client.robot.startCharging();
		client.waitForRecharge();

		// Might as well accept anything up to 12 chars, parser will throw logic error if full power is not received.
		return new MessageValidator(".*", 12);
	}
}

class RobotConfirmationMessage extends RobotMessage {

	/** Accept code obtained from the request. */
	private final char acceptCode;

	public RobotConfirmationMessage(String request) throws SyntaxErrorException {
		try {
			int code = Integer.parseInt(request);

			if (code > Character.MAX_VALUE) {
				throw new SyntaxErrorException();
			}

			this.acceptCode = (char) code;
		}
		catch (NumberFormatException e) {
			throw new SyntaxErrorException();
		}
	}

	@Override
	public MessageValidator handle(Client client) throws LoginFailedException {
		if (!client.authorize(this.acceptCode)) {
			throw new LoginFailedException();
		}
		client.sendMessage(ServerMessage.ok);

		return this.searchUpdate(client);
	}
}

class RobotFullPowerMessage extends RobotMessage {

	public RobotFullPowerMessage(String request) throws LogicErrorException {
		if (!request.matches("FULL POWER")) {
			throw new LogicErrorException();
		}
	}

	@Override
	public MessageValidator handle(Client client) {
		client.robot.stopCharging();
		client.resume();

		// TODO are you sure about that?
		return new MessageValidator(".*", 100);
	}
}

class RobotOkMessage extends RobotMessage {

	/** Regular expression to validate the incoming request against. */
	public static final String REQUEST_REGEX = "^OK -?[0-9]+ -?[0-9]+$";

	/** X Position coordinate, obtained from the request. */
	private final int x;

	/** Y Position coordinate, obtained from the request. */
	private final int y;

	public RobotOkMessage(String request) throws SyntaxErrorException {
		if (!request.matches(REQUEST_REGEX)) {
			throw new SyntaxErrorException();
		}

		String[] parts = request.split(" ");
		this.x = Integer.parseInt(parts[1]);
		this.y = Integer.parseInt(parts[2]);
	}

	@Override
	public MessageValidator handle(Client client) {
		client.robot.moveTo(this.x, this.y);

		return this.searchUpdate(client);
	}
}

class RobotPickUpMessage extends RobotMessage {

	/** Message obtained from the request. */
	private final String message;

	public RobotPickUpMessage(String request) {
		this.message = request;
	}

	@Override
	public MessageValidator handle(Client client) {
		client.robot.search();

		if (!this.message.isEmpty()) {
			// Message found! Disconnect client
			client.sendMessage(ServerMessage.logout);
			client.disconnect();

			return null;
		}

		return this.searchUpdate(client);
	}
}

enum ServerMessage {

	confirmation,
	move("102 MOVE"),
	turnLeft("103 TURN LEFT"),
	turnRight("104 TURN RIGHT"),
	pickUp("105 GET MESSAGE"),
	logout("106 LOGOUT"),
	ok("200 OK"),
	loginFailed("300 LOGIN FAILED"),
	syntaxError("301 SYNTAX ERROR"),
	logicError("302 LOGIC ERROR");

	/** Special sequence indicating end of message. */
	public static final String TERMINATION_SEQUENCE = "\u0007\u0008"; // \a\b

	/** Message from the server. */
	private String message;

	ServerMessage(String message) {
		this.message = message + TERMINATION_SEQUENCE;
	}

	ServerMessage() {
	}

	/**
	 * Creates confirmation message with given accept code.
	 *
	 * @param c Accept code to send (number mod 2^16)
	 * @return Message created.
	 */
	public static ServerMessage confirmation(char c) {
		ServerMessage msg = ServerMessage.confirmation;
		msg.setAcceptCode(c);

		return msg;
	}

	/**
	 * Returns server message based on direction instance.
	 *
	 * @param direction Direction instance.
	 * @return Message created.
	 */
	public static ServerMessage fromDirection(Direction direction) {
		switch (direction) {
			case moveForward: return ServerMessage.move;
			case turnLeft: return ServerMessage.turnLeft;
			case turnRight: return ServerMessage.turnRight;
		}

		return ServerMessage.move;
	}

	@Override
	public String toString() {
		return this.message;
	}

	/**
	 * Assigns accept code as message. Should be only used for instantiating confirmation message.
	 *
	 * @param c Accept code to set.
	 */
	private void setAcceptCode(char c) {
		this.message = Integer.toString((int) c) + TERMINATION_SEQUENCE;
	}
}

class RobotMessageFactory {

	/**
	 * Creates robot message based on current context.
	 *
	 * @param client Client that has sent the message.
	 * @param request Raw message data (without termination sequence).
	 * @return Message instance.
	 * @throws RobotException Invalid message and/or client state.
	 */
	public RobotMessage create(Client client, String request) throws RobotException {
		if (!client.isLoggedIn()) {
			return new RobotUsernameMessage(request);
		}
		if (client.robot.isCharging()) {
			return new RobotFullPowerMessage(request);
		}
		if (request.equals("RECHARGING")) {
			return new RobotRechargingMessage(request);
		}
		if (request.equals("FULL POWER")) {
			return new RobotFullPowerMessage(request);
		}
		if (!client.isAuthorized()) {
			return new RobotConfirmationMessage(request);
		}
		if (request.matches(RobotOkMessage.REQUEST_REGEX)) {
			return new RobotOkMessage(request);
		}

		return new RobotPickUpMessage(request);
	}
}

class Client {

	/** Virtual representation of the client machine. */
	public final Robot robot;

	/** Connection status. */
	private boolean connected;

	/** Connected client socket. */
	private Socket socket;

	/** Hash based on username. */
	private char usernameHash = 0;

	/** Authorization status. */
	private boolean authorized;

	/** Client security key. */
	private final char clientKey;

	/** Server security key. */
	private final char serverKey;

	/** Message output. */
	private PrintWriter output;

	public Client(Socket socket, char serverKey, char clientKey) throws IOException {
		this.output = new PrintWriter(socket.getOutputStream(), true);
		this.robot = new Robot();
		this.socket = socket;
		this.connected = true;
		this.authorized = false;
		this.serverKey = serverKey;
		this.clientKey = clientKey;
	}

	/**
	 * Sends the message back to client. Messages are buffered so ensure flushMessages is called after sending messages.
	 *
	 * @param message Message to send.
	 */
	public void sendMessage(ServerMessage message) {
		this.output.print(message.toString());
		System.out.println("Server: " + message.toString());
	}

	/**
	 * Flushes buffered messages in the output stream.
	 */
	public void flushMessages() {
		this.output.flush();
	}

	/**
	 * Creates stream reader from client socket input stream.
	 *
	 * @return Stream reader.
	 * @throws IOException Socket terminated while or before getting the stream.
	 */
	public BufferedReader createInputReader() throws IOException {
		return new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
	}

	/**
	 * Calculates hash of the given username and sets it to current client.
	 *
	 * @param username Username to calculate the hash of.
	 */
	public void login(String username) {
		this.usernameHash = 0;

		for (int i = 0; i < username.length(); ++i) {
			this.usernameHash += username.charAt(i) * 1000;
		}
	}

	/**
	 * Is logged-in?
	 *
	 * @return TRUE or FALSE.
	 */
	public boolean isLoggedIn() {
		return this.usernameHash > 0;
	}

	/**
	 * Attempts to authorize the client with given client accept code.
	 *
	 * @param acceptCode Client accept code to check.
	 * @return TRUE if authorization was successful, FALSE otherwise.
	 */
	public boolean authorize(char acceptCode) {
		return this.authorized = (this.usernameHash == (char) (acceptCode - this.clientKey));
	}

	/**
	 * Calculates server accept code. Requires log-in.
	 *
	 * @return Accept code calculated.
	 */
	public char serverAcceptCode() {
		return (char) (this.usernameHash + this.serverKey);
	}

	/**
	 * Is logged-in and authorized?
	 *
	 * @return TRUE or FALSE.
	 */
	public boolean isAuthorized() {
		return this.authorized;
	}

	/**
	 * Gets the connection status.
	 *
	 * @return TRUE if client wants to be listened to from the server, FALSE if it wishes to be disconnected.
	 */
	public boolean isConnected() {
		return this.connected;
	}

	/**
	 * Disconnect the client from the server.
	 */
	public void disconnect() {
		this.connected = false;
	}

	/**
	 * Ensures server does not timeout while robot is charging.
	 */
	public void waitForRecharge() {
		try {
			this.socket.setSoTimeout(RobotServer.CLIENT_TIMEOUT_CHARGING);
		}
		catch (SocketException e) {
			// TCP error occurred, connection cannot continue.
			this.disconnect();
		}
	}

	/**
	 * Stops waiting for robot to recharge and resumes normally.
	 */
	public void resume() {
		try {
			this.socket.setSoTimeout(RobotServer.CLIENT_TIMEOUT);
		}
		catch (SocketException e) {
			// TCP error occurred, connection cannot continue.
			this.disconnect();
		}
	}

	/**
	 * Closes active connection.
	 */
	public void close() {
		this.output.close();
	}
}

class RobotServerRunnable implements Runnable {

	/** Factory responsible for creating proper robot message instances, that handle incoming messages. */
	private RobotMessageFactory robotMessageFactory;

	/** Client instance this thread operates with. */
	private Client client;

	/** Client's input stream. */
	private BufferedReader input;

	public RobotServerRunnable(Client client) throws IOException {
		this.client = client;
		this.input = this.client.createInputReader();
		this.robotMessageFactory = new RobotMessageFactory();
	}

	@Override
	public void run() {
		// Validator for the login.
		MessageValidator validator = new MessageValidator(".*", 12);

		while (this.client.isConnected()) {
			try {
				RobotMessage message = this.readMessage(validator);
				validator = message.handle(this.client);
			}
			catch (RobotException e) {
				e.reportTo(this.client);
				this.client.disconnect();
			}
			catch (SocketTimeoutException e) {
				this.client.disconnect();
			}
			catch (IOException e) {
				System.out.println(e.toString());
				this.client.disconnect();
			}

			this.client.flushMessages();
		}

		try {
			this.input.close();
			this.client.close();
		}
		catch (IOException e) {
			System.out.println(e.toString());
		}
	}

	/**
	 * Reads next message from the input stream.
	 *
	 * @param validator Validator, preferably retrieved by the previous message.
	 * @return Message obtained from the input stream.
	 * @throws RobotException Unexpected or incorrect message is received, which cannot be handled.
	 * @throws IOException I/O error on the input stream occurred.
	 */
	private RobotMessage readMessage(MessageValidator validator) throws RobotException, IOException {
		StringBuilder messageBuilder = new StringBuilder(validator.maxLength);

		while (!validator.isComplete(messageBuilder.toString())) {
			messageBuilder.append((char) this.input.read());
		}

		String message = messageBuilder.toString();

		if (!validator.check(message)) {
			throw new SyntaxErrorException();
		}

		// Extract termination sequence.
		message = message.substring(0, message.length() - ServerMessage.TERMINATION_SEQUENCE.length());
		System.out.println("Client: " + message);

		return this.robotMessageFactory.create(this.client, message);
	}
}

class RobotServer {

	/** Timeout for incoming connections. */
	public static final int SERVER_TIMEOUT = 15000;

	/** Default delay that socket waits for input. */
	public static final int CLIENT_TIMEOUT = 1000;

	/** Delay that socket waits for input from a charging robot. */
	public static final int CLIENT_TIMEOUT_CHARGING = 5000;

	/** Server authorization key. */
	private final char serverKey;

	/** Client authorization key. */
	private final char clientKey;

	public RobotServer(int serverKey, int clientKey) {
		this.serverKey = (char) serverKey;
		this.clientKey = (char) clientKey;
	}

	/**
	 * Starts listening on given port.
	 *
	 * @param port Port to listen on.
	 * @throws IOException Socket or stream could not be initialized (see exception message).
	 */
	public void listen(int port) throws IOException {
		ServerSocket serverSocket = new ServerSocket(port);
		serverSocket.setSoTimeout(SERVER_TIMEOUT);

		for (; ; ) {
			try {
				Socket clientSocket = serverSocket.accept();
				clientSocket.setSoTimeout(CLIENT_TIMEOUT);

				Client client = new Client(clientSocket, this.serverKey, this.clientKey);
				RobotServerRunnable clientInstance = new RobotServerRunnable(client);
				Thread clientThread = new Thread(clientInstance);

				clientThread.start();
			}
			catch (SocketTimeoutException e) {
				System.out.println(e.getMessage());
				break;
			}
		}

		serverSocket.close();
	}
}

public class Main {

	/** Port where the server accepts incoming connections. */
	private static final int PORT = 2222;

	/** Server security key. */
	private static final int SERVER_KEY = 54621;

	/** Client security key. */
	private static final int CLIENT_KEY = 45328;

	public static void main(String[] args) {
		RobotServer server = new RobotServer(SERVER_KEY, CLIENT_KEY);

		try {
			server.listen(PORT);
		}
		catch (IOException e) {
			System.out.println(e.toString());
		}
	}
}
