package com.flowcrypt.email.database.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.flowcrypt.email.database.FlowCryptDatabaseManager;
import com.flowcrypt.email.database.FlowCryptSQLiteOpenHelper;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;


/**
 * This class encapsulate data and provide it to the application through the single
 * {@link ContentResolver} interface.
 *
 * @author Denis Bondarenko
 *         Date: 13.05.2017
 *         Time: 10:32
 *         E-mail: DenBond7@gmail.com
 */
public class SecurityContentProvider extends ContentProvider {
    private static final int MATCHED_CODE_KEYS_TABLE = 1;
    private static final int MATCHED_CODE_KEYS_TABLE_SINGLE_ROW = 2;
    private static final int MATCHED_CODE_KEY_CLEAN_DATABASE = 3;
    private static final int MATCHED_CODE_CONTACTS_TABLE = 4;
    private static final int MATCHED_CODE_CONTACTS_TABLE_SINGLE_ROW = 5;

    private static final String SINGLE_APPENDED_SUFFIX = "/#";
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, KeysDaoSource.TABLE_NAME_KEYS,
                MATCHED_CODE_KEYS_TABLE);
        URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, KeysDaoSource.TABLE_NAME_KEYS +
                SINGLE_APPENDED_SUFFIX, MATCHED_CODE_KEYS_TABLE_SINGLE_ROW);
        URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, FlowcryptContract.CLEAN_DATABASE,
                MATCHED_CODE_KEY_CLEAN_DATABASE);
        URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ContactsDaoSource.TABLE_NAME_CONTACTS,
                MATCHED_CODE_CONTACTS_TABLE);
        URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ContactsDaoSource.TABLE_NAME_CONTACTS +
                SINGLE_APPENDED_SUFFIX, MATCHED_CODE_CONTACTS_TABLE_SINGLE_ROW);
    }

    private FlowCryptSQLiteOpenHelper hotelDBHelper;

    public SecurityContentProvider() {
    }

    @Override
    public boolean onCreate() {
        hotelDBHelper = (FlowCryptSQLiteOpenHelper) FlowCryptDatabaseManager.getSqLiteOpenHelper();
        if (hotelDBHelper == null) {
            FlowCryptDatabaseManager.initializeInstance(new FlowCryptSQLiteOpenHelper(getContext
                    ()));
            hotelDBHelper = (FlowCryptSQLiteOpenHelper) FlowCryptDatabaseManager
                    .getSqLiteOpenHelper();
        }
        return true;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Uri result = null;
        if (hotelDBHelper != null) {
            SQLiteDatabase sqLiteDatabase = hotelDBHelper.getWritableDatabase();
            int match = URI_MATCHER.match(uri);

            if (sqLiteDatabase != null) {
                long id;
                switch (match) {
                    case MATCHED_CODE_KEYS_TABLE:
                        id = sqLiteDatabase.insert(new KeysDaoSource().getTableName(), null,
                                values);
                        result = Uri.parse(new KeysDaoSource().getBaseContentUri() + "/" + id);
                        break;

                    case MATCHED_CODE_CONTACTS_TABLE:
                        id = sqLiteDatabase.insert(new ContactsDaoSource().getTableName(), null,
                                values);
                        result = Uri.parse(new ContactsDaoSource().getBaseContentUri() + "/" + id);
                        break;

                    default:
                        throw new UnsupportedOperationException("Unknown uri: " + uri);
                }

                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(uri, null, false);
                }
            }
        }

        return result;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        int insertedRowsCount = 0;

        if (hotelDBHelper != null) {
            SQLiteDatabase sqLiteDatabase = hotelDBHelper.getWritableDatabase();
            if (sqLiteDatabase != null) {
                sqLiteDatabase.beginTransaction();

                int match = URI_MATCHER.match(uri);

                try {
                    switch (match) {
                        case MATCHED_CODE_KEYS_TABLE:
                            for (ContentValues contentValues : values) {
                                long id = sqLiteDatabase.insert(
                                        new KeysDaoSource().getTableName(), null, contentValues);
                                if (id <= 0) {
                                    throw new SQLException("Failed to insert row into " + uri);
                                }
                            }
                            break;

                        case MATCHED_CODE_CONTACTS_TABLE:
                            for (ContentValues contentValues : values) {
                                long id = sqLiteDatabase.insert(
                                        new ContactsDaoSource().getTableName(), null,
                                        contentValues);
                                if (id <= 0) {
                                    throw new SQLException("Failed to insert row into " + uri);
                                }
                            }
                            break;

                        default:
                            throw new UnsupportedOperationException("Unknown uri: " + uri);
                    }

                    insertedRowsCount = values.length;
                    sqLiteDatabase.setTransactionSuccessful();

                    if (getContext() != null) {
                        getContext().getContentResolver().notifyChange(uri, null, false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    sqLiteDatabase.endTransaction();
                }
            }
        }

        return insertedRowsCount;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int rowsCount = -1;
        if (hotelDBHelper != null) {
            SQLiteDatabase sqLiteDatabase = hotelDBHelper.getWritableDatabase();
            int match = URI_MATCHER.match(uri);

            if (sqLiteDatabase != null) {
                switch (match) {
                    case MATCHED_CODE_KEYS_TABLE:
                        rowsCount = sqLiteDatabase.update(
                                new KeysDaoSource().getTableName(),
                                values,
                                selection,
                                selectionArgs);
                        break;

                    case MATCHED_CODE_CONTACTS_TABLE:
                        rowsCount = sqLiteDatabase.update(
                                new ContactsDaoSource().getTableName(),
                                values,
                                selection,
                                selectionArgs);
                        break;

                    default:
                        throw new UnsupportedOperationException("Unknown uri: " + uri);
                }

                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(uri, null, false);
                }
            }
        }

        return rowsCount;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int rowsCount = -1;
        if (hotelDBHelper != null) {
            SQLiteDatabase sqLiteDatabase = hotelDBHelper.getWritableDatabase();
            int match = URI_MATCHER.match(uri);

            if (sqLiteDatabase != null) {
                switch (match) {
                    case MATCHED_CODE_KEY_CLEAN_DATABASE:
                        rowsCount = sqLiteDatabase.delete(
                                new KeysDaoSource().getTableName(), selection, selectionArgs);
                        break;

                    case MATCHED_CODE_CONTACTS_TABLE:
                        rowsCount = sqLiteDatabase.delete(new ContactsDaoSource().getTableName(),
                                selection, selectionArgs);
                        break;

                    default:
                        throw new UnsupportedOperationException("Unknown uri: " + uri);
                }

                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(uri, null, false);
                }
            }
        }

        return rowsCount;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase sqLiteDatabase = hotelDBHelper.getReadableDatabase();

        Cursor cursor;
        String table;
        String groupBy = null;
        String having = null;
        String limit = null;

        switch (URI_MATCHER.match(uri)) {
            case MATCHED_CODE_KEYS_TABLE:
                table = KeysDaoSource.TABLE_NAME_KEYS;
                break;

            case MATCHED_CODE_CONTACTS_TABLE:
                table = ContactsDaoSource.TABLE_NAME_CONTACTS;
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor = sqLiteDatabase.query(table, projection, selection, selectionArgs, groupBy, having,
                sortOrder, limit);

        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null, false);

            if (cursor != null) {
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
            }
        }

        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        final int match = URI_MATCHER.match(uri);
        switch (match) {
            case MATCHED_CODE_KEYS_TABLE:
                return new KeysDaoSource().getRowsContentType();

            case MATCHED_CODE_KEYS_TABLE_SINGLE_ROW:
                return new KeysDaoSource().getSingleRowContentType();

            case MATCHED_CODE_CONTACTS_TABLE:
                return new ContactsDaoSource().getRowsContentType();

            case MATCHED_CODE_CONTACTS_TABLE_SINGLE_ROW:
                return new ContactsDaoSource().getSingleRowContentType();

            default:
                throw new IllegalArgumentException("Unknown uri: " + uri);
        }
    }


}