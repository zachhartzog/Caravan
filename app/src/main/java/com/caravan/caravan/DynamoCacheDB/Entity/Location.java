package com.caravan.caravan.DynamoCacheDB.Entity;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.caravan.caravan.DynamoDB.CuratedDO;

import lombok.Data;

@Data
@Entity(tableName = "locations")
public class Location {
    @PrimaryKey
    @NonNull
    private String id;

    @Embedded
    private CuratedDO curatedDO;

    public Location(String id, CuratedDO curatedDO) {
        this.id=id;
        this.curatedDO=curatedDO;
    }
}
