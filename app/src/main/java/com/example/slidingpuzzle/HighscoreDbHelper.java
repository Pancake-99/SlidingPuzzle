package com.example.slidingpuzzle;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

//manejo de la DB de los records
public class HighscoreDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "highscores.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "scores";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_SIZE = "size";

    //sql para crear la tabla
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT, " +
                    COLUMN_TIME + " TEXT, " +
                    COLUMN_SIZE + " TEXT);";

    public HighscoreDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //se ejecuta la primera vez para armar la tabla
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //limpia todo si cambia la version
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    //guarda un record nuevo
    public void addScore(String name, String time, String size) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_TIME, time);
        values.put(COLUMN_SIZE, size);
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    //trae la lista de records del mas rapido al mas lento
    public List<Score> getAllScores() {
        List<Score> scores = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        //pide los datos ordenados por tiempo
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_TIME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Score score = new Score();
                //esto busca la posicion de la columna por nombre y saca el texto
                score.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                score.setTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)));
                score.setSize(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SIZE)));
                scores.add(score);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return scores;
    }

    //clase para los datos de un record
    public static class Score {
        private String name;
        private String time;
        private String size;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
    }
}
