package test;

import com.google.gson.Gson;

import data.Msg;
import junit.framework.TestCase;

public class MsgTest extends TestCase {
	public void testMsg() {
		Msg msg = new Msg("Hung", "The", "xin chao The");
		Gson gson = new Gson();
		String dataJson = gson.toJson(msg);
		System.out.println(dataJson);

	}
}
