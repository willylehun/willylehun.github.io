package com.byw.monpetitleopard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class PetChooserActivity extends Activity {
    LinearLayout root;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        PetProfileStore.ensureMigrated(this);
        if(PetProfileStore.count(this)==0)showAnimalChoice(PetProfileStore.firstEmpty(this));
        else showProfiles();
    }

    void resetRoot(){
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(18),dp(28),dp(18),dp(22));
        root.setBackgroundColor(Color.rgb(247,240,223));
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root,new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }

    TextView title(String text){
        TextView v=new TextView(this);
        v.setText(text);
        v.setTextColor(Color.rgb(61,49,36));
        v.setTextSize(25);
        v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(6),dp(6),dp(6),dp(14));
        return v;
    }

    TextView subtitle(String text){
        TextView v=new TextView(this);
        v.setText(text);
        v.setTextColor(Color.rgb(112,93,68));
        v.setTextSize(14);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(12),0,dp(12),dp(18));
        return v;
    }

    GradientDrawable cardBackground(){
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(255,253,248));
        g.setCornerRadius(dp(22));
        g.setStroke(dp(1),Color.rgb(216,201,169));
        return g;
    }

    void showProfiles(){
        resetRoot();
        root.addView(title("De qui veux-tu t’occuper aujourd’hui ?"));
        root.addView(subtitle("Choisis un animal. Tu peux avoir jusqu’à 6 compagnons."));

        GridLayout grid=new GridLayout(this);
        grid.setColumnCount(2);
        grid.setRowCount(3);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
        root.addView(grid,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        for(int slot=0;slot<PetProfileStore.MAX_PROFILES;slot++){
            final int s=slot;
            LinearLayout card=new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(8),dp(10),dp(8),dp(10));
            card.setBackground(cardBackground());

            GridLayout.LayoutParams gp=new GridLayout.LayoutParams();
            gp.width=0;
            gp.height=dp(176);
            gp.columnSpec=GridLayout.spec(slot%2,1f);
            gp.rowSpec=GridLayout.spec(slot/2);
            gp.setMargins(dp(5),dp(5),dp(5),dp(5));
            grid.addView(card,gp);

            if(PetProfileStore.exists(this,slot)){
                ImageView icon=new ImageView(this);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                icon.setImageResource(PetProfileStore.iconRes(this,slot));
                card.addView(icon,new LinearLayout.LayoutParams(dp(104),dp(104)));

                TextView name=new TextView(this);
                name.setText(PetProfileStore.name(this,slot));
                name.setTextColor(Color.rgb(61,49,36));
                name.setTextSize(17);
                name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                name.setGravity(Gravity.CENTER);
                card.addView(name);

                TextView sex=new TextView(this);
                sex.setText(PetProfileStore.speciesLabel(this,slot)+" • "+
                    PetProfileStore.sexLabel(PetProfileStore.sex(this,slot)));
                sex.setTextColor(Color.rgb(110,94,72));
                sex.setTextSize(11);
                sex.setGravity(Gravity.CENTER);
                card.addView(sex);
                card.setOnClickListener(v->selectExisting(s));
            }else{
                TextView plus=new TextView(this);
                plus.setText("+");
                plus.setTextSize(48);
                plus.setTextColor(Color.rgb(102,112,67));
                plus.setGravity(Gravity.CENTER);
                card.addView(plus,new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
                TextView add=new TextView(this);
                add.setText("Nouvel animal");
                add.setTextColor(Color.rgb(61,49,36));
                add.setTextSize(14);
                add.setGravity(Gravity.CENTER);
                card.addView(add);
                card.setOnClickListener(v->showAnimalChoice(s));
            }
        }
    }

    void selectExisting(int slot){
        String sex=PetProfileStore.sex(this,slot);
        if(sex==null||sex.isEmpty()){
            showSexChoice(slot,false,PetProfileStore.species(this,slot));
            return;
        }
        launchProfile(slot);
    }

    void showAnimalChoice(int slot){
        if(!PetProfileStore.validSlot(slot)||PetProfileStore.exists(this,slot)){
            showProfiles();
            return;
        }
        resetRoot();
        root.setPadding(dp(18),dp(16),dp(18),dp(16));
        root.addView(title("Choisis ton animal"));
        root.addView(subtitle("Choisis ton nouveau compagnon."));

        GridLayout choices=new GridLayout(this);
        choices.setColumnCount(2);
        choices.setRowCount(3);
        choices.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        choices.setUseDefaultMargins(false);
        root.addView(choices,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        addAnimalCard(choices,slot,0,PetSpecies.LEOPARD);
        addAnimalCard(choices,slot,1,PetSpecies.WOLF);
        addAnimalCard(choices,slot,2,PetSpecies.TIGER);
        addAnimalCard(choices,slot,3,PetSpecies.LION);
        addAnimalCard(choices,slot,4,PetSpecies.FOX);
        addAnimalCard(choices,slot,5,PetSpecies.BEAR);

        if(PetProfileStore.count(this)>0){
            Button back=button("← Retour aux animaux");
            back.setOnClickListener(v->showProfiles());
            root.addView(back,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(50)));
        }
    }

    void addAnimalCard(GridLayout choices,int slot,int index,String species){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8),dp(8),dp(8),dp(8));
        card.setBackground(cardBackground());
        GridLayout.LayoutParams cp=new GridLayout.LayoutParams();
        cp.width=0;
        cp.height=ViewGroup.LayoutParams.WRAP_CONTENT;
        cp.columnSpec=GridLayout.spec(index%2,1f);
        cp.rowSpec=GridLayout.spec(index/2);
        cp.setMargins(dp(5),0,dp(5),dp(10));
        choices.addView(card,cp);

        ImageView icon=new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        // The species card is only a preview; both lion sexes are shown on the next screen.
        icon.setImageResource(PetSpecies.iconRes(species,"male",0L));
        card.addView(icon,new LinearLayout.LayoutParams(dp(72),dp(72)));

        LinearLayout labels=new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER);
        card.addView(labels,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView name=new TextView(this);
        name.setText(PetSpecies.label(species));
        name.setTextColor(Color.rgb(61,49,36));
        name.setTextSize(22);
        name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        name.setGravity(Gravity.CENTER);
        labels.addView(name);

        TextView ready=new TextView(this);
        ready.setText(PetSpecies.isLion(species)?"Lion ou lionne":
            PetSpecies.stageLabel(species,MainActivity.PetStage.CUB));
        ready.setTextColor(Color.rgb(96,112,59));
        ready.setTextSize(13);
        ready.setGravity(Gravity.CENTER);
        labels.addView(ready);

        card.setContentDescription("Adopter un "+PetSpecies.label(species).toLowerCase(Locale.FRANCE));
        card.setOnClickListener(v->showSexChoice(slot,true,species));
    }

    void showSexChoice(int slot,boolean creating,String species){
        resetRoot();
        root.addView(title("Choisis le sexe de l’animal"));
        if(PetSpecies.isLion(species)){
            root.addView(subtitle("Choisis un lion ou une lionne."));
            long age=creating?0L:Math.max(0L,System.currentTimeMillis()-
                getSharedPreferences(PetProfileStore.petPrefsName(slot),MODE_PRIVATE)
                    .getLong("born",System.currentTimeMillis()));
            LinearLayout choices=new LinearLayout(this);
            choices.setOrientation(LinearLayout.HORIZONTAL);
            root.addView(choices,new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
            addLionSexCard(choices,slot,creating,"male",age);
            addLionSexCard(choices,slot,creating,"female",age);
        }else{
            root.addView(subtitle("Ce choix sera affiché sur son profil."));
            ImageView icon=new ImageView(this);
            icon.setImageResource(creating?PetSpecies.iconRes(species,0L):PetProfileStore.iconRes(this,slot));
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            root.addView(icon,new LinearLayout.LayoutParams(dp(170),dp(170)));

            Button male=button("♂  Mâle");
            Button female=button("♀  Femelle");
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,dp(58));
            bp.setMargins(0,dp(8),0,dp(8));
            root.addView(male,bp);
            root.addView(female,bp);

            male.setOnClickListener(v->afterSex(slot,creating,species,"male"));
            female.setOnClickListener(v->afterSex(slot,creating,species,"female"));
        }

        Button back=button(creating?"← Choisir un autre animal":"← Retour aux animaux");
        back.setOnClickListener(v->{if(creating)showAnimalChoice(slot);else showProfiles();});
        root.addView(back,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,dp(50)));
    }

    void addLionSexCard(LinearLayout choices,int slot,boolean creating,String sex,long age){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(6),dp(8),dp(6),dp(8));
        card.setBackground(cardBackground());
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(
            0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);
        cp.setMargins(dp(4),0,dp(4),dp(14));
        choices.addView(card,cp);

        ImageView icon=new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImageResource(PetSpecies.iconRes(PetSpecies.LION,sex,age));
        icon.setContentDescription(PetSpecies.label(PetSpecies.LION,sex));
        card.addView(icon,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,dp(118)));

        Button choose=button("female".equals(sex)?"♀  Femelle":"♂  Mâle");
        card.addView(choose,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,dp(58)));
        choose.setOnClickListener(v->afterSex(slot,creating,PetSpecies.LION,sex));
        card.setOnClickListener(v->afterSex(slot,creating,PetSpecies.LION,sex));
    }

    void afterSex(int slot,boolean creating,String species,String sex){
        if(creating)showNameChoice(slot,species,sex);
        else{
            PetProfileStore.setSex(this,slot,sex);
            launchProfile(slot);
        }
    }

    void showNameChoice(int slot,String species,String sex){
        resetRoot();
        root.addView(title("Quel est son nom ?"));
        root.addView(subtitle("Tu pourras retrouver ce nom dans la sélection des animaux."));

        ImageView icon=new ImageView(this);
        icon.setImageResource(PetSpecies.iconRes(species,sex,0L));
        icon.setContentDescription(PetSpecies.label(species,sex));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(icon,new LinearLayout.LayoutParams(dp(160),dp(160)));

        EditText input=new EditText(this);
        input.setSingleLine(true);
        input.setHint(PetSpecies.defaultName(species,sex));
        input.setTextSize(20);
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,dp(64));
        ip.setMargins(0,dp(10),0,dp(14));
        root.addView(input,ip);

        String article=PetSpecies.isLion(species)&&"female".equals(sex)?"cette ":
            PetSpecies.isBear(species)?"cet ":"ce ";
        Button confirm=button("Adopter "+article+PetSpecies.label(species,sex).toLowerCase(Locale.FRANCE));
        root.addView(confirm,new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,dp(58)));
        confirm.setOnClickListener(v->{
            if(PetProfileStore.exists(this,slot)){
                Toast.makeText(this,"Cet emplacement est déjà occupé.",Toast.LENGTH_SHORT).show();
                showProfiles();
                return;
            }
            confirm.setEnabled(false);
            String name=input.getText().toString().trim();
            if(name.isEmpty())name=PetSpecies.defaultName(species,sex);
            PetProfileStore.createAnimal(this,slot,species,sex,name);
            launchProfile(slot);
        });

        Button back=button("← Choisir le sexe");
        back.setOnClickListener(v->showSexChoice(slot,true,species));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,dp(50));
        bp.setMargins(0,dp(10),0,0);
        root.addView(back,bp);
    }

    Button button(String label){
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        GradientDrawable g=new GradientDrawable();
        g.setColor(Color.rgb(102,112,67));
        g.setCornerRadius(dp(16));
        b.setBackground(g);
        return b;
    }

    void launchProfile(int slot){
        Intent intent=new Intent(this,MainActivity.class);
        intent.putExtra(PetProfileStore.EXTRA_SLOT,slot);
        startActivity(intent);
        finish();
    }

    int dp(int value){
        return Math.round(value*getResources().getDisplayMetrics().density);
    }
}
