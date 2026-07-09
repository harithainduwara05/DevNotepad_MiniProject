package com.devnotepad.editor.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.devnotepad.editor.data.local.dao.DocumentDao;
import com.devnotepad.editor.data.local.dao.DocumentDao_Impl;
import com.devnotepad.editor.data.local.dao.VersionSnapshotDao;
import com.devnotepad.editor.data.local.dao.VersionSnapshotDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DevNotepadDatabase_Impl extends DevNotepadDatabase {
  private volatile DocumentDao _documentDao;

  private volatile VersionSnapshotDao _versionSnapshotDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `documents` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `file_path` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `last_modified_at` INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_documents_file_path` ON `documents` (`file_path`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `version_snapshots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `document_id` INTEGER NOT NULL, `version_number` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `patch_data` TEXT NOT NULL, `description` TEXT, FOREIGN KEY(`document_id`) REFERENCES `documents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_version_snapshots_document_id_version_number` ON `version_snapshots` (`document_id`, `version_number`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3024f50a46c7b411945733ca75efb0f2')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `documents`");
        db.execSQL("DROP TABLE IF EXISTS `version_snapshots`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsDocuments = new HashMap<String, TableInfo.Column>(5);
        _columnsDocuments.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocuments.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocuments.put("file_path", new TableInfo.Column("file_path", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocuments.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDocuments.put("last_modified_at", new TableInfo.Column("last_modified_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDocuments = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDocuments = new HashSet<TableInfo.Index>(1);
        _indicesDocuments.add(new TableInfo.Index("index_documents_file_path", false, Arrays.asList("file_path"), Arrays.asList("ASC")));
        final TableInfo _infoDocuments = new TableInfo("documents", _columnsDocuments, _foreignKeysDocuments, _indicesDocuments);
        final TableInfo _existingDocuments = TableInfo.read(db, "documents");
        if (!_infoDocuments.equals(_existingDocuments)) {
          return new RoomOpenHelper.ValidationResult(false, "documents(com.devnotepad.editor.data.local.entity.DocumentEntity).\n"
                  + " Expected:\n" + _infoDocuments + "\n"
                  + " Found:\n" + _existingDocuments);
        }
        final HashMap<String, TableInfo.Column> _columnsVersionSnapshots = new HashMap<String, TableInfo.Column>(6);
        _columnsVersionSnapshots.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVersionSnapshots.put("document_id", new TableInfo.Column("document_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVersionSnapshots.put("version_number", new TableInfo.Column("version_number", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVersionSnapshots.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVersionSnapshots.put("patch_data", new TableInfo.Column("patch_data", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVersionSnapshots.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVersionSnapshots = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysVersionSnapshots.add(new TableInfo.ForeignKey("documents", "CASCADE", "NO ACTION", Arrays.asList("document_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesVersionSnapshots = new HashSet<TableInfo.Index>(1);
        _indicesVersionSnapshots.add(new TableInfo.Index("index_version_snapshots_document_id_version_number", true, Arrays.asList("document_id", "version_number"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoVersionSnapshots = new TableInfo("version_snapshots", _columnsVersionSnapshots, _foreignKeysVersionSnapshots, _indicesVersionSnapshots);
        final TableInfo _existingVersionSnapshots = TableInfo.read(db, "version_snapshots");
        if (!_infoVersionSnapshots.equals(_existingVersionSnapshots)) {
          return new RoomOpenHelper.ValidationResult(false, "version_snapshots(com.devnotepad.editor.data.local.entity.VersionSnapshotEntity).\n"
                  + " Expected:\n" + _infoVersionSnapshots + "\n"
                  + " Found:\n" + _existingVersionSnapshots);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "3024f50a46c7b411945733ca75efb0f2", "084830e82eb56f91c691f60347590634");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "documents","version_snapshots");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `documents`");
      _db.execSQL("DELETE FROM `version_snapshots`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(DocumentDao.class, DocumentDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(VersionSnapshotDao.class, VersionSnapshotDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public DocumentDao documentDao() {
    if (_documentDao != null) {
      return _documentDao;
    } else {
      synchronized(this) {
        if(_documentDao == null) {
          _documentDao = new DocumentDao_Impl(this);
        }
        return _documentDao;
      }
    }
  }

  @Override
  public VersionSnapshotDao versionSnapshotDao() {
    if (_versionSnapshotDao != null) {
      return _versionSnapshotDao;
    } else {
      synchronized(this) {
        if(_versionSnapshotDao == null) {
          _versionSnapshotDao = new VersionSnapshotDao_Impl(this);
        }
        return _versionSnapshotDao;
      }
    }
  }
}
