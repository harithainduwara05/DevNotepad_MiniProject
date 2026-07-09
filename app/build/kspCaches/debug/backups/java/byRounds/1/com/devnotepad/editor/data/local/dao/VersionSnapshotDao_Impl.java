package com.devnotepad.editor.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.devnotepad.editor.data.local.entity.VersionSnapshotEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class VersionSnapshotDao_Impl implements VersionSnapshotDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<VersionSnapshotEntity> __insertionAdapterOfVersionSnapshotEntity;

  private final EntityDeletionOrUpdateAdapter<VersionSnapshotEntity> __deletionAdapterOfVersionSnapshotEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAfterVersion;

  public VersionSnapshotDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfVersionSnapshotEntity = new EntityInsertionAdapter<VersionSnapshotEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `version_snapshots` (`id`,`document_id`,`version_number`,`timestamp`,`patch_data`,`description`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VersionSnapshotEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getDocumentId());
        statement.bindLong(3, entity.getVersionNumber());
        statement.bindLong(4, entity.getTimestamp());
        statement.bindString(5, entity.getPatchData());
        if (entity.getDescription() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDescription());
        }
      }
    };
    this.__deletionAdapterOfVersionSnapshotEntity = new EntityDeletionOrUpdateAdapter<VersionSnapshotEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `version_snapshots` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VersionSnapshotEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAfterVersion = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        DELETE FROM version_snapshots \n"
                + "        WHERE document_id = ? \n"
                + "          AND version_number > ?\n"
                + "        ";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final VersionSnapshotEntity snapshot,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfVersionSnapshotEntity.insertAndReturnId(snapshot);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final VersionSnapshotEntity snapshot,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfVersionSnapshotEntity.handle(snapshot);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAfterVersion(final long documentId, final int afterVersion,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAfterVersion.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, documentId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, afterVersion);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAfterVersion.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<VersionSnapshotEntity>> observeByDocument(final long documentId) {
    final String _sql = "\n"
            + "        SELECT * FROM version_snapshots \n"
            + "        WHERE document_id = ? \n"
            + "        ORDER BY version_number ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, documentId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"version_snapshots"}, new Callable<List<VersionSnapshotEntity>>() {
      @Override
      @NonNull
      public List<VersionSnapshotEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDocumentId = CursorUtil.getColumnIndexOrThrow(_cursor, "document_id");
          final int _cursorIndexOfVersionNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "version_number");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfPatchData = CursorUtil.getColumnIndexOrThrow(_cursor, "patch_data");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final List<VersionSnapshotEntity> _result = new ArrayList<VersionSnapshotEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VersionSnapshotEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDocumentId;
            _tmpDocumentId = _cursor.getLong(_cursorIndexOfDocumentId);
            final int _tmpVersionNumber;
            _tmpVersionNumber = _cursor.getInt(_cursorIndexOfVersionNumber);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpPatchData;
            _tmpPatchData = _cursor.getString(_cursorIndexOfPatchData);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            _item = new VersionSnapshotEntity(_tmpId,_tmpDocumentId,_tmpVersionNumber,_tmpTimestamp,_tmpPatchData,_tmpDescription);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSnapshotsUpTo(final long documentId, final int upToVersion,
      final Continuation<? super List<VersionSnapshotEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM version_snapshots \n"
            + "        WHERE document_id = ? \n"
            + "          AND version_number <= ? \n"
            + "        ORDER BY version_number ASC\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, documentId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, upToVersion);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VersionSnapshotEntity>>() {
      @Override
      @NonNull
      public List<VersionSnapshotEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDocumentId = CursorUtil.getColumnIndexOrThrow(_cursor, "document_id");
          final int _cursorIndexOfVersionNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "version_number");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfPatchData = CursorUtil.getColumnIndexOrThrow(_cursor, "patch_data");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final List<VersionSnapshotEntity> _result = new ArrayList<VersionSnapshotEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VersionSnapshotEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDocumentId;
            _tmpDocumentId = _cursor.getLong(_cursorIndexOfDocumentId);
            final int _tmpVersionNumber;
            _tmpVersionNumber = _cursor.getInt(_cursorIndexOfVersionNumber);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpPatchData;
            _tmpPatchData = _cursor.getString(_cursorIndexOfPatchData);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            _item = new VersionSnapshotEntity(_tmpId,_tmpDocumentId,_tmpVersionNumber,_tmpTimestamp,_tmpPatchData,_tmpDescription);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getLatestSnapshot(final long documentId,
      final Continuation<? super VersionSnapshotEntity> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM version_snapshots \n"
            + "        WHERE document_id = ? \n"
            + "        ORDER BY version_number DESC \n"
            + "        LIMIT 1\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, documentId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<VersionSnapshotEntity>() {
      @Override
      @Nullable
      public VersionSnapshotEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDocumentId = CursorUtil.getColumnIndexOrThrow(_cursor, "document_id");
          final int _cursorIndexOfVersionNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "version_number");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfPatchData = CursorUtil.getColumnIndexOrThrow(_cursor, "patch_data");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final VersionSnapshotEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDocumentId;
            _tmpDocumentId = _cursor.getLong(_cursorIndexOfDocumentId);
            final int _tmpVersionNumber;
            _tmpVersionNumber = _cursor.getInt(_cursorIndexOfVersionNumber);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpPatchData;
            _tmpPatchData = _cursor.getString(_cursorIndexOfPatchData);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            _result = new VersionSnapshotEntity(_tmpId,_tmpDocumentId,_tmpVersionNumber,_tmpTimestamp,_tmpPatchData,_tmpDescription);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object countVersions(final long documentId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM version_snapshots WHERE document_id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, documentId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
