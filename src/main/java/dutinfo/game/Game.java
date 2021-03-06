package dutinfo.game;

import dutinfo.console.App;
import dutinfo.game.environment.Island;
import dutinfo.game.environment.Season;
import dutinfo.game.events.Action;
import dutinfo.game.events.Event;
import dutinfo.game.events.Scenario;
import dutinfo.game.society.Faction;
import dutinfo.game.society.Field;
import dutinfo.console.App.Color;
import dutinfo.game.society.President;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;

public class Game {

	private static final boolean jarMode = false;

	public enum Difficulty {
		EASY(0.5f, 10f), NORMAL(1f, 20f), HARD(2f, 50f);

		private float multiplier;
		private float minGlobalSatisfaction;

		Difficulty(float multiplier, float minGlobalSatisfaction) {
			this.multiplier = multiplier;
			this.minGlobalSatisfaction = minGlobalSatisfaction;
		}

		public float getMultiplier(){
			return multiplier;
		}

		public double getMinGlobalSatisfaction(){
			return minGlobalSatisfaction;
		}
	}

	private Island island;
	private Difficulty difficulty;
	private HashMap<Integer, List<Event>> events;
	/*
	 * The events stack (next ev to pick if correspond to season) <------< (last ev
	 * append to stack)
	 */
	private List<Event> nextEvents; // event stack
	private Event event; // Current / last event
	private Scenario scenario; // Current scenario

	private static List<Faction> FACTIONS; // All factions in init file
	private static List<Field> FIELDS; // All field in init file
	private static List<Scenario> SCENARIOS; // All Scenarios in folders
	public static final float monthlyPriceForDept = 0.3f; // on total value
	public static final int bribingPrice = 15;
	public static final int foodPrice = 8;

	public Game(List<Faction> factions, List<Field> fields, List<Scenario> scenarios,
				HashMap<Integer, List<Event>> events) {
		this.FACTIONS = factions;
		this.events = events;
		this.FIELDS = fields;
		this.SCENARIOS = scenarios;
		this.difficulty = Difficulty.NORMAL;
		nextEvents = new ArrayList<>();

	}

	public void setDifficulty(Difficulty difficulty) {
		Objects.requireNonNull(difficulty, "diffuculty can't be null");
		this.difficulty = difficulty;
	}

	public Island getIsland() {
		return this.island;
	}

	/**
	 * set the island of the game. Init the parameters of the islands
	 * @param islandName
	 * @param president
	 */
	public void setIsland(String islandName, President president) {

		Objects.requireNonNull(scenario, "Can't process new island without scenario");

		//set the factions
		List<Faction> facs = FACTIONS;
		facs.stream().forEach(x -> {
			x.setApprobationPercentage(scenario.getSatisFaction(x.getId()));
			x.setNbrSupporters(scenario.getFollowers());
		});

		//set the fields
		List<Field> fields = FIELDS;
		fields.stream().forEach(x -> {

			var c = (100 - (fields.parallelStream().filter(y -> y.getId() != x.getId()).findFirst().get().getExploitationPercentage())); // reste
			///System.out.println((c < scenario.getExploitField(x.getId())) ? c: scenario.getExploitField(x.getId()));
			x.setExploitationPercentage((Math.min(c, scenario.getExploitField(x.getId()))));
			x.setYieldPercentage(scenario.getFieldYieldPercentage(x.getId()));
		});

		//set

		island = new Island(islandName, president, facs, fields, scenario.getTreasure(), scenario.getFood());
	}

	public void setScenario(Scenario scenario) {
		this.scenario = scenario;
	}

	public static String getFactionNameById(int id){
		try{
			Faction f =  FACTIONS.stream().filter(x -> x.getId() == id).findFirst().get();
			return f.getName();
		}catch (Exception e){
			return "";
		}
	}

	public static String getFieldNameById(int id){
		try{
			Field f =  FIELDS.stream().filter(x -> x.getId() == id).findFirst().get();
			return f.getName();
		}catch (Exception e){
			return "";
		}
	}

	public Scenario getScenario() {
		return this.scenario;
	}

	public Field getFieldByName(String name) {
		return FIELDS.stream().filter(x -> x.getName().equals(name)).findFirst().get();
	}

	public Faction getFactionByName(String name) {
		return FACTIONS.stream().filter(x -> x.getName().equals(name)).findFirst().get();
	}

	/**
	 * @return all the scenarios found in game folders
	 */
	public List<Scenario> getScenarios() {
		return SCENARIOS;
	}

	/**
	 * @return all the factions set in init file
	 */
	public List<Faction> getFactions() {
		return FACTIONS;
	}

	/**
	 * @return all the fields setted in game folders
	 */
	public List<Field> getFields() {
		return FIELDS;
	}

	/**
	 * get all the events by the current scenario and the common ones
	 *
	 * @param scenario we use the current scenario to use this function
	 */
	public List<Event> getEvents(Scenario scenario) {
		List<Event> eventsList = new ArrayList<>();
		events.keySet().forEach(x -> {
			if (scenario.getPackageIds().contains(x)) {
				eventsList.addAll(events.get(x));
			}
		});

		return eventsList;
	}

	/**
	 * Check if the player lose the game
	 *
	 * @return
	 */
	public boolean checkLose() {
		//System.out.println(island.totalSupporters());
		//System.out.println(island.globalSatisfaction()+" < "+ difficulty.getMinGlobalSatisfaction());
		return !(island.globalSatisfaction() > difficulty.getMinGlobalSatisfaction());
	}

	/**
	 * pass the turn, set the new current event, increment island year
	 * @return false if the player lost
	 */
	public boolean nextTurn() {

		event = null;
		// add total random new event to stack
		addNextEvents();
		// get the next valid event and set it has the new current event
		setCurrentEvent(pickNextEvents());
		//update the season of the island
		island.incrementSeason();
		if(island.getSeason() == Season.Spring){
			island.incrementYears();
		}
		return !checkLose();
	}

	/**
	 * Get the current event - Can be null ! in this case the player can just pass a
	 * turn
	 */
	public Event getEvent() {
		return event;
	}

	/**
	 * add an event from the scenario to the event stack
	 */
	public void addNextEvents() {
		var ev = scenario.getRandomEvent();
		if (ev == null) {
			return;
		}
		nextEvents.add(ev);
	}

	/**
	 * Get the next event from the stack corresponding to the current season or with
	 * no bound season. Can return null.
	 *
	 * @return
	 */
	public Event pickNextEvents() {
		try {
			Event ev = nextEvents.stream().filter(x -> x.getSeason() == island.getSeason() || x.isOnlyARepercussion() || x.getSeason() == null )
					.findFirst().get();
			nextEvents.remove(ev);
			return ev;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * set the given event as the current events
	 * @param event
	 */
	public void setCurrentEvent(Event event) {
		this.event = event;
	}

	/**
	 * play the given action by applying difficulty coef
	 * @param action
	 */
	public void playAction(Action action){
		float coef = difficulty.getMultiplier();
		island.updateFactions(action.getFactionsEffects(), coef);
		island.updateFields(action.getFieldsEffects(), coef);
		island.updateFoodUnits(action.getFood()*coef);
		island.updateTreasure(action.getTreasure()*coef);
		//append a repercussion if exist to the stack
		Event ev = scenario.getEventById(action.getRandomRepercussionId());
		if(ev != null) nextEvents.add(ev);
	}

	/**
	 * Get the correspondent faction from the id on its empty form
	 *
	 * @param id Id of the faction
	 * @return
	 */
	public Faction getFactionById(int id) {
		return (Faction) FACTIONS.stream().filter(f -> f.getId() == id);
	}


	/**
	 * Master Control Program This nested class store all errors of the game from
	 * the init of the files to the end of the party.
	 */
	public static class MCP {
		private static HashMap<String, Set<String>> failedEventsFiles;
		private static String errorReporter = "";

		/**
		 * add all the data from parsing error in the field assigned for it in the class
		 *
		 * @param files all the files name by their Scenario name if the parsing failed
		 */
		public static void addFailedEventFile(HashMap<String, Set<String>> files) {
			failedEventsFiles.putAll(files);
		}

		public static void addErrorToReporter(String str){
			errorReporter+="[info] "+str+"\n";
		}

		public static String print() {
			StringBuilder sb = new StringBuilder();
			if (failedEventsFiles != null) {
				sb.append("[Failed scenarios files to read]");
				failedEventsFiles.forEach((x, y) -> {
					sb.append("| Scenario\n");
					y.forEach(name -> sb.append("\t").append(name).append("\n"));
				});
			}
			return sb.toString();
		}

	}

	/**
	 * Parse all json files needed for the game
	 *
	 * @return a Game object
	 */
	public static Game initGame() {


		String pathToFactionsFile;
		String pathToFieldsFile;
		String pathToScenariosDir;
		String pathToEventsDir;

		if(jarMode){ // will use absolute path of the jar to look for external resources : set jarMode to true
			String pathToData = ""; try{ String jarPath = Game.class
					.getProtectionDomain() .getCodeSource() .getLocation() .toURI() .getPath();
				File file = new File(jarPath); pathToData =
						file.getParentFile().getPath()+"\\resources\\";


			} catch (URISyntaxException e) { e.printStackTrace(); }

			pathToFactionsFile = "factions.json";
			pathToFieldsFile = "fields.json";
			pathToScenariosDir = pathToData+"scenarios\\";
			pathToEventsDir = pathToData+"events\\";

		}else{
			pathToFactionsFile = "factions.json";
			pathToFieldsFile = "fields.json";
			pathToScenariosDir = ".\\src\\main\\resources\\scenarios";
			pathToEventsDir = ".\\src\\main\\resources\\events\\";
		}

		/* WHEN MAKIN JAR


		*/

		/* Paths */



		/* Init factions */
		List<Faction> factions = Faction.initFaction(pathToFactionsFile);

		/* Init fields */
		List<Field> fields = Field.initField(pathToFieldsFile);

		/* Init scenarios */
		List<Scenario> scenarios = Scenario.initScenarios(pathToScenariosDir);

		/* Init events from packages */
		// <package id, event list>
		HashMap<Integer, List<Event>> events = Event.initEvents(pathToEventsDir);
		// à décommenter pour voir les evnmts/ events.values().forEach(x ->
		// x.forEach(System.out::println));

		// System.out.println(events);
		Game game = new Game(factions, fields, scenarios, events);

		Game.MCP.print();

		return game;
	}


	public boolean canBuyFood(int amount){
		int price = (Game.foodPrice * amount);
		if(price > island.getTreasury()){
			return false;
		}
		return true;
	}

	public void buyFood(int amount){
		int price = (Game.foodPrice * amount);
		if(price > island.getTreasury()) return;
		island.updateFoodUnits(amount);
		island.updateTreasure(price*(-1));
	}

	public String updateResourcesEndofYear(){
		String str = "";
		float thenTreasure = island.getTreasury();
		int npeople = island.updatePopulationAndTreasury();
		float nowTreasure = island.getTreasury();
		if(thenTreasure > nowTreasure){
			str+= "- Your are in deficit you lost some money to pay the debts. "+(nowTreasure-thenTreasure)+"\n";
		}

		if(npeople > 0){
			str+= "- You had not enough food to feed all the population properly, some of them died. -"+npeople+" citizens. The approbation of all the factions has decreased.\n";
		}else if(npeople < 0){
			str+= "- You had more food than needed, some citizens appeared. +"+npeople+" citizens.\n";
		}

		return str;
	}

	public String generateYearlyResources(){
		String str = "";
		float thenTreasure = island.getTreasury();
		float thenFood= island.getFoodUnits();
		island.generateYearlyResources();
		float nowTreasure = island.getTreasury();
		float nowFood = island.getFoodUnits();

		if((nowTreasure - thenTreasure) > 0){
			str+="- Your capital evolved thanks to your industry: +"+GameUtils.round((nowTreasure - thenTreasure), 2)+"$.\n";
		}
		if((nowFood - thenFood) > 0){
			str+="- Your number of food units evolved thanks to your agriculture: +"+GameUtils.round((nowFood - thenFood), 2)+" units.\n";
		}


		return str;
	}
}