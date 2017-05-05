package pkgPoker.app.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

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
import pkgPokerEnum.eCardVisibility;
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
				
				
				/*
				 * I assume the dealer is whoever pressed the button?
				 * 
				 * This tries to set the dealer to the player from act
				 * if it can't it randomly assigns
				 */
				if(actPlayer.getPlayerName() == null){
					ArrayList<Player> pList = new ArrayList<Player>();
					pList.addAll(HubPokerTable.getHmPlayer().values());
					
					Random rand = new Random();
					HubGamePlay = new GamePlay(rle, pList.get(rand.nextInt(pList.size()) + 1).getPlayerID());
					
				}
				else{
					HubGamePlay = new GamePlay(rle, actPlayer.getPlayerID());
				}
				
				
				
				HubGamePlay.setGamePlayers(HubPokerTable.getHmPlayer());
				
				
				
				int jokers = rle.GetNumberOfJokers();
				ArrayList<Card> wilds = rle.GetWildCards();
				HubGamePlay.setGameDeck(new Deck(jokers, wilds));
				
				
				
				//The order of the players, depending on which player hit the start button
				int[] playerOrder = GamePlay.GetOrder(actPlayer.getiPlayerPosition());
				
				HubGamePlay.setiActOrder(playerOrder);
				


			case Draw:


				Rule rl = new Rule(act.geteGame());
				int minCommCards = rl.getCommunityCardsMin();
				int playerMax = rl.getPlayerCardsMax();
				
				
				ArrayList<Player> pList = new ArrayList<Player>();
				pList.addAll(HubGamePlay.getGamePlayers().values());
				
				TreeMap tm =  rl.getHmCardDraw();
				Collection c = tm.values();
				Iterator it = c.iterator();
				Iterator itr = rl.getHmCardDraw().entrySet().iterator();
				
				/*
				 * The cards found in Rule rl have set visibilities depending on their order
				 * 
				 * Iterate through each player, and each card to draw
				 * set visibility depending on the current count in correspondence to the rules
				 * 
				 * 
				 */
				for(Player p : pList){
					
					int currentHandCount = HubGamePlay.getPlayerHand(p).getCardsInHand().size();
					
					for(int i = currentHandCount; i <= playerMax; i++){
						/*
						 * Just trying to iterate through the TreeMap in Rule rl
						 * Cause depending on the card order, some are visible and some aren't
						 * I think this works
						 * 
						 */
						while(itr.hasNext()){
							Map.Entry<Integer, CardDraw> tmc = (Map.Entry<Integer, CardDraw>)itr.next();
							
							if(tmc.getValue().getCardVisibility() == eCardVisibility.VisibleEveryone){
								HubGamePlay.drawCard(p, eCardDestination.Player, eCardVisibility.VisibleEveryone);
							}
							else{
								HubGamePlay.drawCard(p, eCardDestination.Player, eCardVisibility.VisibleMe);
							}
						}
						
						
					}
				}
				
				if(iDealNbr == 1) 
					HubGamePlay.seteDrawCountLast(eDrawCount.FIRST);
				else if(iDealNbr == 2) 
					HubGamePlay.seteDrawCountLast(eDrawCount.SECOND);
				else if(iDealNbr == 3) 
					HubGamePlay.seteDrawCountLast(eDrawCount.THIRD);
				else if(iDealNbr == 4) 
					HubGamePlay.seteDrawCountLast(eDrawCount.FOURTH);
				else if(iDealNbr== 5) 
					HubGamePlay.seteDrawCountLast(eDrawCount.FIFTH);
				else
					HubGamePlay.seteDrawCountLast(eDrawCount.SIXTH);
				
				
				
				//GameCommonHand isn't always instantiated so I just put a try catch
				int currentCommCount = 0;
				try{
					currentCommCount = HubGamePlay.getGameCommonHand().getCardsInHand().size();
				}
				catch(java.lang.NullPointerException e){
					currentCommCount = 0;
				}
				
				
				//Community cards should always be visible I assume
				for(int i = currentCommCount; i <= minCommCards; i++){
					HubGamePlay.drawCard(new Player(), eCardDestination.Community, eCardVisibility.VisibleEveryone);
				}
				
				
				
				
				HubGamePlay.isGameOver();
				resetOutput();
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