package dev.qixils.crowdcontrol.socket;

import dev.qixils.crowdcontrol.socket.Response.PacketType;
import dev.qixils.crowdcontrol.socket.Response.ResultType;

class DummyResponse implements JsonObject {
	int id = 0;
	ResultType status;
	String message;
	long timeRemaining;
	PacketType type;

	public String toJSON() {
		return ByteAdapter.GSON.toJson(this);
	}
}
