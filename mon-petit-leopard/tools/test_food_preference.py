from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/byw/monpetitleopard"
MAIN = JAVA / "PetBehavior.java"

shared_preferences_stub = """package android.content;
public interface SharedPreferences {
    boolean getBoolean(String key, boolean fallback);
    long getLong(String key, long fallback);
    int getInt(String key, int fallback);
    String getString(String key, String fallback);
    Editor edit();
    interface Editor {
        Editor putString(String key, String value);
        Editor putBoolean(String key, boolean value);
        Editor putLong(String key, long value);
        Editor putInt(String key, int value);
        Editor remove(String key);
        void apply();
    }
}
"""

contract_test = """package com.byw.monpetitleopard;
import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

final class FoodPreferenceContractTest {
    static final class MemoryPreferences implements SharedPreferences {
        final Map<String,Object> values = new HashMap<>();
        public boolean getBoolean(String key, boolean fallback) {
            Object value=values.get(key); return value instanceof Boolean?(Boolean)value:fallback;
        }
        public long getLong(String key, long fallback) {
            Object value=values.get(key); return value instanceof Long?(Long)value:fallback;
        }
        public int getInt(String key, int fallback) {
            Object value=values.get(key); return value instanceof Integer?(Integer)value:fallback;
        }
        public String getString(String key, String fallback) {
            Object value=values.get(key); return value instanceof String?(String)value:fallback;
        }
        public Editor edit() {
            return new Editor() {
                public Editor putString(String key,String value){values.put(key,value);return this;}
                public Editor putBoolean(String key,boolean value){values.put(key,value);return this;}
                public Editor putLong(String key,long value){values.put(key,value);return this;}
                public Editor putInt(String key,int value){values.put(key,value);return this;}
                public Editor remove(String key){values.remove(key);return this;}
                public void apply(){}
            };
        }
    }

    static void check(boolean condition,String message) {
        if(!condition) throw new AssertionError(message);
    }
    static void preference(MemoryPreferences prefs,String id,boolean cub,
                           PetBehavior.Preference expected) {
        check(PetBehavior.foodPreference(prefs,id,cub)==expected,
              id+" at cub="+cub+" should be "+expected);
    }

    public static void main(String[] args) throws Exception {
        MemoryPreferences prefs=new MemoryPreferences();
        prefs.values.put("personality_food_likes","bottle,milk,junior,kibble");
        prefs.values.put("personality_food_dislikes","wet");

        // Bottle is neutral at every life stage, even if an old saved profile lists it.
        for(boolean cub:new boolean[]{true,false})
            preference(prefs,"bottle",cub,PetBehavior.Preference.NEUTRAL);

        // Milk and junior food ignore saved tastes only while the pet is a cub.
        preference(prefs,"milk",true,PetBehavior.Preference.NEUTRAL);
        preference(prefs,"junior",true,PetBehavior.Preference.NEUTRAL);
        preference(prefs,"milk",false,PetBehavior.Preference.LOVE);
        preference(prefs,"junior",false,PetBehavior.Preference.LOVE);
        preference(prefs,"wet",true,PetBehavior.Preference.DISLIKE);
        preference(prefs,"wet",false,PetBehavior.Preference.DISLIKE);
        preference(prefs,"kibble",true,PetBehavior.Preference.LOVE);
        preference(prefs,"kibble",false,PetBehavior.Preference.LOVE);

        Field field=PetBehavior.class.getDeclaredField("FOOD_IDS");
        field.setAccessible(true);
        String[] pool=(String[])field.get(null);
        boolean hasMilk=false,hasJunior=false;
        for(String id:pool){hasMilk|="milk".equals(id);hasJunior|="junior".equals(id);}
        check(hasMilk && hasJunior,"milk and junior must be in the generated personality pool");

        System.out.println("Food preferences by age: PASS");
    }
}
"""

with tempfile.TemporaryDirectory() as temp:
    temp = Path(temp)
    stub = temp / "android/content/SharedPreferences.java"
    test = temp / "com/byw/monpetitleopard/FoodPreferenceContractTest.java"
    stub.parent.mkdir(parents=True)
    test.parent.mkdir(parents=True)
    stub.write_text(shared_preferences_stub)
    test.write_text(contract_test)
    subprocess.run(
        ["javac", "-d", str(temp), str(stub), str(MAIN), str(test)],
        check=True,
    )
    subprocess.run(
        ["java", "-cp", str(temp), "com.byw.monpetitleopard.FoodPreferenceContractTest"],
        check=True,
    )
