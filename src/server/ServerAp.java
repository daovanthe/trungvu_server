package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.Gson;

import data.Msg;
import data.User;

public class ServerAp {
	private static Map<SocketChannel, Queue<ByteBuffer>> pendingData = new HashMap<SocketChannel, Queue<ByteBuffer>>();

	// static Selector selector;
	private final static String HOSTNAME = "192.168.3.103";
	private final static int PORT_THSERVER = 1901;
	private static HashMap<String, SocketChannel> IdMapLiveChannel = new HashMap<String, SocketChannel>();
	private static Set<String> IdMapUser = new HashSet<String>();
	private static Selector selector;
	final static Object object = new Object();

	public static void main(String[] args) throws IOException {

		// create user
		new Thread() {
			public void run() {
				synchronized (object) {
					try {
						object.wait();
						Scanner scanner = new Scanner(System.in);
						while (true) {
							System.out.println("you can create an User: please create ID");
							String IdUser = scanner.nextLine();
							IdMapUser.add(IdUser);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

		// run server on thread
		new Thread(() -> {
			try {
				Log.Logger.d("main", "run Server OK!");

				Log.Logger.d("main", "thread notify");
				startServer();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();

	}

	private static void startServer() throws IOException {
		// TODO Auto-generated method stub
		selector = Selector.open();
		System.out.println("NonblockingSingleThreadedPollingServer open");
		synchronized (object) {
			object.notify();
		}
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress(HOSTNAME, PORT_THSERVER));

		ssc.configureBlocking(false);
		ssc.register(selector, SelectionKey.OP_ACCEPT);
		stringInputBuffer = new StringBuffer();

		while (true) {
			selector.select();
			for (Iterator<SelectionKey> itKeys = selector.selectedKeys().iterator(); itKeys.hasNext();) {
				SelectionKey key = itKeys.next();

				if (key.isValid()) {
					if (key.isAcceptable()) { // some one coneected to our
												// server coketchannel
						accept(key);
					} else if (key.isReadable()) {
						read(key);
					} else if (key.isWritable()) {
						write(key);
					}
				}
			}
		}
	}

	private static void accept(SelectionKey key) throws IOException {
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc = ssc.accept();
		if (sc == null) {
			return;
		}
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_READ);
		key.cancel();
		pendingData.put(sc, new ConcurrentLinkedQueue<ByteBuffer>());
		// add pending data entry ... Dont foget

	}

	private static StringBuffer stringInputBuffer;
	private static StringBuffer stringOutputBuffer;

	private static void write(SelectionKey key) throws IOException {
		SocketChannel socket = (SocketChannel) key.channel();
		Queue<ByteBuffer> queue = pendingData.get(socket);
		ByteBuffer buf = ByteBuffer.allocate(4096);
		while ((buf = queue.peek()) != null) {
			stringInputBuffer.setLength(0);
			for (int i = 0; i < buf.limit(); i++) {
				// buf.put(i, (byte) Util.transmogrify(buf.get(i)));
				buf.put(i, (byte) buf.get(i));
			}
			stringInputBuffer.append(new String(buf.array(), "UTF-8").trim());
			System.out.println("write function: " + stringInputBuffer);

			stringOutputBuffer = Util.transmogrify(stringInputBuffer);

			buf.clear();
			buf.put(stringOutputBuffer.toString().getBytes(Charset.forName("UTF-8")));
			buf.flip();
			socket.write(buf);

			queue.poll();

			if (!buf.hasRemaining()) {
				queue.poll();
			} else {
				return;
			}
		}
		socket.register(key.selector(), SelectionKey.OP_READ);
	}

	private static Gson gson = new Gson();
	static ByteBuffer byteBufer = ByteBuffer.allocate(1024);
	private static void read(SelectionKey key) throws IOException {
//		Log.Logger.d("read", "readed");
		SocketChannel socket = (SocketChannel) key.channel();
		
		int read = socket.read(byteBufer);
		if (read == -1) {
			pendingData.remove(socket);
			return;
		}
		byteBufer.rewind();
		byteBufer.flip();
		StringBuffer lStringBuffer = new StringBuffer();
		for (int i = 0; i < byteBufer.limit(); i++) {
			// buf.put(i, (byte) Util.transmogrify(buf.get(i)));
			byteBufer.put(i, (byte) byteBufer.get(i));
			lStringBuffer.append((char) (byte) byteBufer.get(i));
		}
		System.out.println(lStringBuffer.toString());
		// String convert
		Msg message = gson.fromJson(lStringBuffer.toString(), Msg.class);
		if (message != null) {
			String fromUserId = message.getFromUser();
			String toUserId = message.getTo();
			String contentMessage = message.getMsg();
			// check the from user
			if (IdMapUser.contains(fromUserId)) {
				// if exist and make user online
				if (!IdMapLiveChannel.containsKey(fromUserId)) {
					IdMapLiveChannel.put(fromUserId, socket);
				}
			}

			// find the destination Channel to send Msg;
			if (IdMapLiveChannel.containsKey(toUserId)) {
				SocketChannel receiveChannel = IdMapLiveChannel.get(toUserId);
				writeDataToChannel(receiveChannel, message);
			} else {
				Log.Logger.d("readServer", "can not find user to send Message");
				// if (IdMapUser.contains(toUserId))
				// Log.Logger.d("readServer", "checking of user " + toUserId);
			}
		}
		pendingData.get(socket).add(byteBufer);
		byteBufer.clear();
	}

	private static void writeDataToChannel(SocketChannel receiveChannel, Msg wholeMessage) throws IOException {
		Log.Logger.d("writeDataToChannel", "write");
		ByteBuffer buf = ByteBuffer.allocate(4096);

		stringInputBuffer.setLength(0);
		for (int i = 0; i < buf.limit(); i++) {
			// buf.put(i, (byte) Util.transmogrify(buf.get(i)));
			buf.put(i, (byte) buf.get(i));
		}
		stringInputBuffer.append(wholeMessage.getFromUser() + " to ");
		stringInputBuffer.append(wholeMessage.getFromUser() + " : ");
		stringInputBuffer.append(wholeMessage.getMsg());
		System.out.println(stringInputBuffer);

		buf.clear();
		buf.put((wholeMessage.getFromUser() + ": " + wholeMessage.getMsg()).getBytes(Charset.forName("UTF-8")));
		buf.flip();
		receiveChannel.write(buf);

		// check user exist ornot

		receiveChannel.register(selector, SelectionKey.OP_READ);

	}

}
