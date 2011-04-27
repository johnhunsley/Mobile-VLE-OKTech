package com.mobilevle.oktech.session;

import android.content.Context;
import android.content.ContentValues;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.util.Log;
import com.mobilevle.core.moodle.User;


/**
 * <p></p>
 *
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * </p>
 *
 * @author johnhunsley
 *         Date: 11-Nov-2010
 *         Time: 13:34:17
 */
public class SessionDAOSQLiteImpl implements SessionDAO {
    private static final String DATABASE_NAME = "mobilevle_db";
    private static final int DATABASE_VERSION = 1;
    private static final String SESSION_TABLE = "`SESSION`";
    private static final String USERNAME = "USERNAME";
    private static final String PASSWORD = "PASSWORD";
    private static final String USERID = "USER_ID";
    private static final String CLIENTID = "CLIENT_ID";
    private static final String TOKEN = "TOKEN";
	private Context context;
	private DatabaseHelper dbHelper;

	private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

    	/*
    	 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
    	 */
    	@Override
    	public void onCreate(SQLiteDatabase db) {
    		db.execSQL("create table "+SESSION_TABLE+" ("+USERNAME+" text primary key, "+PASSWORD+" text, "+USERID+" text not null, "+CLIENTID+" integer, "+TOKEN+" text);");
       	}

    	/*
    	 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
    	 */
    	@Override
    	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    		db.execSQL("drop table if exists "+SESSION_TABLE+";");
    		onCreate(db);
    	}
    }

    /**
     *
     * @param context
     */
	public SessionDAOSQLiteImpl(Context context) {

		if(context == null) throw new IllegalArgumentException("Context is null");

    	this.context = context;
    	open();
	}

	/**
	 *
	 * @return this
	 */
	public SessionDAOSQLiteImpl open() {
		if(dbHelper == null || !dbHelper.getWritableDatabase().isOpen())
			dbHelper = new DatabaseHelper(this.context);

		return this;
	}

    /**
     * <p>Replace the existing {@link Session}</p>
     * @param session
     * @param savePassword
     */
    public synchronized void saveSession(final Session session, final boolean savePassword) {
        Log.i("SessionDAOSQLiteImpl",
                "saveSession()....username - "+session.getUsername()+" password - "+session.getPassword());
        SQLiteDatabase database = dbHelper.getWritableDatabase();
		ContentValues contentValues = new ContentValues();
		contentValues.put(USERNAME, session.getUsername());

        if(savePassword) contentValues.put(PASSWORD, session.getPassword());

        contentValues.put(USERID, session.getUserId());
        contentValues.put(CLIENTID, session.getClientId());
        contentValues.put(TOKEN, session.getToken());
		database.beginTransaction();
        database.delete(SESSION_TABLE, null, null);
        database.insert(SESSION_TABLE, null, contentValues);
		database.setTransactionSuccessful();
		database.endTransaction();
        database.close();
        Log.i("SessionDAOSQLiteImpl", "saveSession() - transaction complete");
    }

    /**
	 *<p>Delete the existing {@link Session}.</p>	 *
	 */
	private void deleteSession() {
		SQLiteDatabase database = dbHelper.getWritableDatabase();
		database.beginTransaction();
		database.delete(SESSION_TABLE, null, null);
		database.setTransactionSuccessful();
		database.endTransaction();
        database.close();
	}


    /**
     *  todo - fix this, tis caused by threads treading on each other in the messenger app
     *  E/AndroidRuntime(28481): java.lang.IllegalStateException: database /data/data/com.mobilevle.messenger/databases/mobilevle_db already closed
E/AndroidRuntime(28481):        at android.database.sqlite.SQLiteCompiledSql.<init>(SQLiteCompiledSql.java:58)
E/AndroidRuntime(28481):        at android.database.sqlite.SQLiteProgram.<init>(SQLiteProgram.java:80)
E/AndroidRuntime(28481):        at android.database.sqlite.SQLiteQuery.<init>(SQLiteQuery.java:46)
E/AndroidRuntime(28481):        at android.database.sqlite.SQLiteDirectCursorDriver.query(SQLiteDirectCursorDriver.java:53)
E/AndroidRuntime(28481):        at android.database.sqlite.SQLiteDatabase.rawQueryWithFactory(SQLiteDatabase.java:1412)
E/AndroidRuntime(28481):        at android.database.sqlite.SQLiteDatabase.rawQuery(SQLiteDatabase.java:1382)
E/AndroidRuntime(28481):        at com.mobilevle.oktech.session.SessionDAOSQLiteImpl.loadSession(SessionDAOSQLiteImpl.java:124)
E/AndroidRuntime(28481):        at com.mobilevle.oktech.VLEHandlerOKTechImpl.getCourses(VLEHandlerOKTechImpl.java:218)
E/AndroidRuntime(28481):        at com.mobilevle.messenger.ContactSync.sync(ContactSync.java:41)
E/AndroidRuntime(28481):        at com.mobilevle.messenger.ContactSyncService$1$1.run(ContactSyncService.java:56)
     * @return {@link Session}
     */
    public synchronized Session loadSession() {
        final String sql = "select "+USERNAME+", "+PASSWORD+", "+USERID+", "+CLIENTID+", "+TOKEN+" from "+SESSION_TABLE;
        SQLiteDatabase database = dbHelper.getWritableDatabase();
		Cursor cursor = database.rawQuery(sql, null);
        Session session = null;

        if(cursor.moveToFirst()) {
            session = new Session();
            session.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(USERNAME)));
            session.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(PASSWORD)));
            session.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(USERID)));
            session.setClientId(cursor.getInt(cursor.getColumnIndexOrThrow(CLIENTID)));
            session.setToken(cursor.getString(cursor.getColumnIndexOrThrow(TOKEN)));
        }

        cursor.close();
        database.close();
        return session;
    }

}
