package pkgPoker.app.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import netgame.common.Hub;
import pkgPokerBLL.Action;
import pkgPokerBLL.Card;
import pkgPokerBLL.CardDraw;
import pkgPokerBLL.Deck;
import pkgPokerBLL.GamePlay;
import pkgPokerBLL.GamePlayPlayerHand;
import pkgPokerBLL.Player;
import pkgPokerBLL.Rule;
import pkgPokerBLL.Table;

import pkgPokerEnum.eAction;
import pkgPokerEnum.eCardDestination;
import pkgPokerEnum.eDrawCount;
import pkgPokerEnum.eGame;
import pkgPokerEnum.eGameState;

public class PokerHub extends Hub {

	private Table HubPokerTable = new Table();
	private GamePlay HubGamePlay;
	private int iDealNbr = 0;

	public PokerHub(int port) throws IOException {
		super(port);
	}

	protected void playerConnected(int playerID) {

		if (playerID == 2) {
			shutdownServerSocket();
		}
	}

	protected void playerDisconnected(int playerID) {
		shutDownHub();
	}

	protected void messageReceived(int ClientID, Object message) {

		if (message instanceof Action) {
			Player actPlayer = (Player) ((Action) message).getPlayer();
			Action act = (Action) message;
			switch (act.getAction()) {
			case Sit:
				HubPokerTable.AddPlayerToTable(actPlayer);
				resetOutput();
				sendToAll(HubPokerTable);
				break;
			case Leave:			
				HubPokerTable.RemovePlayerFromTable(actPlayer);
				resetOutput();
				sendToAll(HubPokerTable);
				break;
			case TableState:
				resetOutput();
				sendToAll(HubPokerTable);
				break;
			case StartGame:
				// Get the rule from the Action object.
				Rule rle = new Rule(act.geteGame());
				
				//TODO Lab #5 - If neither player has 'the button', pick a random player
				//		and assign the button.				

				//TODO Lab #5 - Start the new instance of GamePlay
								
				// Add Players to Game
				
				// Set the order of players
				
				
				//I assume "the button" is whoever the dealer is? (or whoever pressed the button) Very ambiguous
				
				if(actPlayer.getPlayerName() == null){	//Will be null if neither player pressed the button
					ArrayList<Player> pList = new ArrayList<Player>();
					pList.addAll(HubPokerTable.getHmPlayer().values());
					
					Random rand = new Random();
					HubGamePlay = new GamePlay(rle, pList.get(rand.nextInt(pList.size()) + 1).getPlayerID());
					
				}
				else{	//Otherwise just make the game with the act player
					HubGamePlay = new GamePlay(rle, actPlayer.getPlayerID());
				}
				
				//Sets the players in the current Table to the current Game
				HubGamePlay.setGamePlayers(HubPokerTable.getHmPlayer());
				
				
				//Sets a newly created deck to the Game
				int jokers = rle.GetNumberOfJokers();
				ArrayList<Card> wilds = rle.GetWildCards();

				HubGamePlay.setGameDeck(new Deck(jokers, wilds));
				
				//The order of the players, depending on which player hit the start button. I think.
				int[] playerOrder = GamePlay.GetOrder(actPlayer.getiPlayerPosition());
				
				HubGamePlay.setiActOrder(playerOrder);
				


			case Draw:

				//TODO Lab #5 -	Draw card(s) for each player in the game.
				//TODO Lab #5 -	Make sure to set the correct visiblity
				//TODO Lab #5 -	Make sure to account for community cards


				Rule rl = new Rule(act.geteGame());
				int minCommCards = rl.getCommunityCardsMin();
				int playerMax = rl.getPlayerCardsMax();
				
				
				
				
				ArrayList<Player> pList = new ArrayList<Player>();
				pList.addAll(HubGamePlay.getGamePlayers().values());
				
				for(Player p : pList){
					
					int currentHandCount = HubGamePlay.getPlayerHand(p).getCardsInHand().size();
					
					for(int i = currentHandCount; i <= playerMax; i++){
						
						
						
						HubGamePlay.drawCard(p, eCardDestination.Player);
					}
				}
				
				
				int currentCommCount = 0;
				try{
					currentCommCount = HubGamePlay.getGameCommonHand().getCardsInHand().size();
				}
				catch(java.lang.NullPointerException e){
					currentCommCount = 0;
				}
				for(int i = currentCommCount; i <= minCommCards; i++){
					HubGamePlay.drawCard(new Player(), eCardDestination.Community);
				}
				
				
				HubGamePlay.seteDrawCountLast(eDrawCount.NONE);

				//TODO Lab #5 -	Check to see if the game is over
				HubGamePlay.isGameOver();
				
				resetOutput();
				//	Send the state of the gameplay back to the clients
				sendToAll(HubGamePlay);
				break;
			case ScoreGame:
				// Am I at the end of the game?

				resetOutput();
				sendToAll(HubGamePlay);
				break;
			}
			
		}

	}

}