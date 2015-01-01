package com.example.Demo_Subj;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



/**
 * Created by johannes on 22.11.2014.
 */

public class Subject {

    //aktuelle Position
    private float xPos;
    private float yPos;
    private int aktRoomID = 0;

    //Ziel
    private float xDest;
    private int destRoomID;

    //Bildverweise
    //TODO: List/Array of bitmaps for animations
    private Bitmap subjStandBitmap;
    private Bitmap subjStandBitmapInv;
    private Dijkstra dijkstra;

    private List<Bitmap> subjectWalk;
    private List<Bitmap> subjectWalkInv;
    private List<Room> route;
    private int routeRoomNum = 0;

    private Bitmap aktBitmap;

    private int listPointerWalk = 0;
    private int holdAnimation = 0;
    private static int holdAnimationCycles = 20;

    private Context context;
    private MediaPlayer mediaplayer;

    private Intelligence intel;
    
    public Subject(Bitmap sStand, List<Bitmap> bitmaps, Context c){
        subjStandBitmap = Bitmap.createScaledBitmap(sStand, GlobalInformation.getScreenWidth()/5, (int)(GlobalInformation.getScreenHeight()/1.5), false);
        subjStandBitmapInv = mirrorBitmap(subjStandBitmap);

        aktBitmap = sStand;
        context = c;

        //Copy List of Bitmaps for Walking animations
        subjectWalk = new ArrayList<Bitmap>();
        subjectWalkInv = new ArrayList<Bitmap>();

        for (int i = 0; i <= (bitmaps.size() - 1); i++){
            Bitmap bm = bitmaps.get(i);
            subjectWalk.add(i,  Bitmap.createScaledBitmap(bm, GlobalInformation.getScreenWidth()/5, (int)(GlobalInformation.getScreenHeight()/1.5), false));
        }

        for (int i = 0; i <= (subjectWalk.size() - 1); i++){
            Bitmap bm = subjectWalk.get(i);
            bm = this.mirrorBitmap(bm);
            subjectWalkInv.add(i, bm);
        }

        float canvasx = (float) GlobalInformation.getScreenWidth();
        float canvasy = (float) GlobalInformation.getScreenHeight();
        float bitmapx = (float) subjStandBitmap.getWidth();
        float bitmapy = (float) subjStandBitmap.getHeight();
        float posX = ((canvasx/2) - (bitmapx / 2));
        float posY = (((canvasy/2) - (bitmapy / 2)) + (canvasy/15));
        setDefaultKoords(posX, posY, 0);

        intel = new Intelligence();

        mediaplayer = new MediaPlayer();

        dijkstra = new Dijkstra();
    }

    private void startSound(int soundRes){
        Uri path = Uri.parse("android.resource://com.example.Demo_Subj/" + soundRes);
        mediaplayer = new MediaPlayer();
        mediaplayer.reset();
        try{
            mediaplayer.setDataSource(context, path);
        }catch (IOException e){
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        try{
            mediaplayer.prepare();
        }catch (IOException e){
            e.printStackTrace();
        }catch (IllegalStateException e) {
            e.printStackTrace();
        }

        Thread sound = new Thread(new Runnable() {
            @Override
            public void run() {
            mediaplayer.start();
            mediaplayer.setVolume(1.0f, 1.0f);

            AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume (AudioManager.STREAM_MUSIC), 0);

            while (mediaplayer.getCurrentPosition() != mediaplayer.getDuration()) {

            }
            mediaplayer.start();
            mediaplayer.setVolume(1,1);
            }
        });
        sound.start();
    }

    private void setDefaultKoords(float xDef, float yDef, int room) {
        xPos = xDef;
        yPos = yDef;
        aktRoomID = room;

        xDest = xDef;
        destRoomID = room;
    }

    //kann das Ziel des Subjektes verändern
    public void setDest(float x, int room){
        xDest = x;
        destRoomID = room;
    }

    //gibt die Y Koordinate im Raum zurück an der sich das Subjekt gerade befindet
    public float getxPos() {
        return xPos;
    }

    //gibt die Y Koordinate im Raum zurück an der sich das Subjekt gerade befindet
    public float getyPos() {
        return yPos;
    }

    //gibt den Raum zurück in dem sich das Subjekt gerade befindet
    public int getAktRoomID() {
        return aktRoomID;
    }

    public Bitmap getSubjBitmap(){
        return aktBitmap;
    }

    public void tick() {
        intel.tick();

        // TODO hier sollte das Animations-Zeugs eigentlich nicht gemacht werden, nur die Bewegung
        // die Animation sollte das GraphicalOutput dann aus dem Zustand und der Richtung erzeugen (oder?)
        if((xPos == xDest) && (aktRoomID == destRoomID)){
            routeRoomNum = 0;
            aktBitmap = subjStandBitmap;
            listPointerWalk = 0;
            // we finished the last action, so get the next from the KI
            //ToDO: Schnittstelle muss auf getNextObject() umgestellt werdne; ist aber noch nicht genau spezifiziert
            SubjectMoveAction nextAction = intel.getNextAction();
            setDest(nextAction.getDestX(), nextAction.getDestRoom());
            route = dijkstra.dijkstra(World.getRoomByIdJS(aktRoomID), World.getRoomByIdJS(destRoomID));
            // move.getDestItem().use();
        }
        else if (aktRoomID == destRoomID) {
            if (xPos > xDest) {
                swapBitmap(false);
                xPos --;
            }
            else{
                swapBitmap(true);
                xPos++;
            }
        }
        else if (aktRoomID != destRoomID){
            if (xPos == 0){
                aktBitmap = subjStandBitmap;
                routeRoomNum++;
                aktRoomID = route.get(routeRoomNum).getId();
                xPos = GlobalInformation.getScreenWidth() - 1;//kann nicht GlobalInformation.getScreenWidth() sein sonst geht die Fkt unten wieder rein
                //reset walking animation
                listPointerWalk = 0;
                startSound(R.raw.sound_door);
            }
            else if (xPos == (GlobalInformation.getScreenWidth())){
                aktBitmap = subjStandBitmapInv;
                routeRoomNum++;
                aktRoomID = route.get(routeRoomNum).getId();
                xPos = 1;//kann nicht 0 sein sonst geht die Fkt oben wieder rein (xPos == 0)
                //reset walking animation
                listPointerWalk = 0;
                startSound(R.raw.sound_door);
            }
            else if ((xPos == (GlobalInformation.getScreenWidth())) &&
                    ((route.get(routeRoomNum + 1).getId() == World.getRoomById(aktRoomID).getLowerRoomID()) ||
                     (route.get(routeRoomNum + 1).getId() == World.getRoomById(aktRoomID).getUpperRoomID()))){
                aktBitmap = subjStandBitmapInv;
                routeRoomNum++;
                aktRoomID = route.get(routeRoomNum).getId();
                xPos = 1;//kann nicht 0 sein sonst geht die Fkt oben wieder rein (xPos == 0)
                //reset walking animation
                listPointerWalk = 0;
                startSound(R.raw.sound_door);
            }
            else{
                if (route.get(routeRoomNum + 1).getId() == World.getRoomByIdJS(aktRoomID).getRightRoomID()){
                    swapBitmap(true);
                    xPos++;
                }
                else if (route.get(routeRoomNum + 1).getId() == World.getRoomByIdJS(aktRoomID).getLeftRoomID()){
                    swapBitmap(false);
                    xPos--;
                }
            }
        }
    }

    private void swapBitmap(boolean mirror){
        if (mirror == true){
            if (holdAnimation < holdAnimationCycles){
                holdAnimation++;
            }
            else{
                holdAnimation = 0;
                aktBitmap = subjectWalkInv.get(listPointerWalk);
                listPointerWalk++;
                if (listPointerWalk > (subjectWalkInv.size() - 1)){
                    listPointerWalk = 0;
                }
            }
        }
        else
            if (holdAnimation < holdAnimationCycles){
                holdAnimation++;
            }
            else {
                holdAnimation = 0;
                aktBitmap = subjectWalk.get(listPointerWalk);
                listPointerWalk++;
                if (listPointerWalk > (subjectWalk.size() - 1)) {
                    listPointerWalk = 0;
                }
            }
    }

    private Bitmap mirrorBitmap(Bitmap b){
        Matrix matrixMirror = new Matrix();
        matrixMirror.preScale(-1.0f, 1.0f);
        b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrixMirror, false);
        return b;
    }
}
