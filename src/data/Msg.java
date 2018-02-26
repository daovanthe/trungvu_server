package data;

public class Msg {

	// FIELD
	public String fromUser;
	public String toUser;
	public String msg;

	// constructor
	public Msg(String fromUser, String toUser, String msg) {
		super();
		this.fromUser = fromUser;
		this.toUser = toUser;
		this.msg = msg;
	}

	// Apis
	public String getFromUser() {
		return fromUser;
	}

	public void setFromUser(String fromUser) {
		this.fromUser = fromUser;
	}

	public String getToUser() {
		return toUser;
	}

	public void setToUser(String toUser) {
		this.toUser = toUser;
	}

	public String getTo() {
		return toUser;
	}

	public void setTo(String to) {
		this.toUser = to;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

}
