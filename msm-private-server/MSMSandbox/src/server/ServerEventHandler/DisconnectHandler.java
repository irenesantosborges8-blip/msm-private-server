package server.ServerEventHandler;

import com.smartfoxserver.v2.core.ISFSEvent;
import com.smartfoxserver.v2.core.SFSEventParam;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.exceptions.SFSException;
import com.smartfoxserver.v2.extensions.BaseServerEventHandler;

import server.MainExtension;
import server.Settings;
import server.Entities.Player;
import server.Tools.Util;

public class DisconnectHandler extends BaseServerEventHandler {

	@Override
	public void handleServerEvent(ISFSEvent event) throws SFSException {
		User user = (User) event.getParameter(SFSEventParam.USER);
		Settings.QUEUE--;
		
		Player player = (Player) user.getProperty("player_object");
		
		if (player == null || player.user_game_id == null) {
			return;
		}
		
		String userGameId = player.user_game_id;
		
		/*
		
		int user_count = getParentExtension().getParentZone().getUserCount();
		
        Util.PostRequest(
        	    "https://discord.com/api/webhooks/1388224087003889834/WR8a9JEcMCv9tmQGkXJPsoueXyX1tuoE6aYhr6yh3rvpHda95lNCkWzrFAPtOJx1S5H3",
        	    "{\"content\": \"new online is: " + user_count + "/" + Settings.max_user_count + "\"}"
        	);
		*/
		
		MainExtension.sqlHandler.query("UPDATE players SET coins = ? WHERE id = ?", player.coins, userGameId);
		MainExtension.sqlHandler.query("UPDATE players SET food = ? WHERE id = ?", player.food, userGameId);
		MainExtension.sqlHandler.query("UPDATE players SET diamonds = ? WHERE id = ?", player.diamonds, userGameId);
		MainExtension.sqlHandler.query("UPDATE players SET `keys` = ? WHERE id = ?", player.keys, userGameId);
		MainExtension.sqlHandler.query("UPDATE players SET shards = ? WHERE id = ?", player.shards, userGameId);
		MainExtension.sqlHandler.query("UPDATE players SET relics = ? WHERE id = ?", player.relics, userGameId);
		MainExtension.sqlHandler.query("UPDATE players SET starpower = ? WHERE id = ?", player.starpower, userGameId);
		
		user.removeProperty("player_object");
	}

}
