package dut.info.console;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

public class Scenario {
    private final int id;
    private final String title;
    private final int generalSatisfaction;
    private final HashMap<Integer, Integer> facPercentage;
    private final int followers;
    private final HashMap<Integer, Integer> filPercentage;
    private final int treasure;


    private Scenario(String title, int generalSatisfaction, HashMap<Integer, Integer> facPercentage, int followers, HashMap<Integer, Integer> filPercentage, int treasure) {
        this.title = Objects.requireNonNull(title);
        assert title.isEmpty() != true;
        this.id = GameUtils.idByHashString(title);
        this.generalSatisfaction = generalSatisfaction;
        this.facPercentage = facPercentage;
        this.filPercentage = filPercentage;
        this.followers = followers;
        this.treasure = treasure;
    }

    /** Return the list of all scenarios objects
     * @param pathToScenarioDir Path to the scenarios directory
     * */
    public static ArrayList<Scenario> initScenarios(String pathToScenarioDir){

        // All scenarios json
        List<File> scenariosJson = GameUtils.allJsonFromDir(Paths.get(pathToScenarioDir).toFile());
        // All scenarios
        List<Scenario> scenarios = new ArrayList<>();

        // All files failed to parse for each scenario
        Set<String> failedFiles = new HashSet<>();

        scenariosJson.stream().forEach(f -> {
            // Global array of the file
            JSONObject ar = (JSONObject) ((Object) GameUtils.jsonToObject(f.getPath()));

            //Title
            String title = f.getName().substring(0, f.getName().lastIndexOf("."));


            //Satisfaction and exploitation
            JSONObject satisfaction = (JSONObject) ((Object) ar.get("satisfaction"));
            int generalSatisfaction = (int) (long) satisfaction.get("general");
            JSONArray factionException = (JSONArray) satisfaction.get("exceptions");
            HashMap<Integer, Integer> facPercentage = new HashMap<>();

            factionException.forEach(fac -> {
                JSONObject exc = (JSONObject) fac;
                facPercentage.put(
                        GameUtils.idByHashString((String) exc.get("name")),
                        (int) (long) exc.get("percentage")
                );
            });

            //Followers
            int followers = (int) (long) ar.get("followers");

            //Fields
            JSONArray fields = (JSONArray) ar.get("fields");
            HashMap<Integer, Integer> filPercentage = new HashMap<>();
            fields.forEach(fi -> {
                JSONObject exc = (JSONObject) fi;
                filPercentage.put(
                        GameUtils.idByHashString((String) exc.get("name")),
                        (int) (long) exc.get("percentage")
                );
            });
            //Treasure
            int treasure = (int) (long) ar.get("treasure");

            scenarios.add(new Scenario(title, generalSatisfaction, facPercentage, followers, filPercentage, treasure));
        } );

        System.out.println(scenarios.get(0));
        return new ArrayList<>();
    }

    @Override
    public String toString(){
        String str = "\nScenario " + title + "\n";
        str += "General satisfaction : "+generalSatisfaction+ "\n";
        str += "Followers by factions : "+followers+"\n";
        str += "Starting treasure : "+treasure+"\n";
        return str;
    }
}
