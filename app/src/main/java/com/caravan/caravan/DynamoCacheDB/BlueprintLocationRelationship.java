package com.caravan.caravan.DynamoCacheDB;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import lombok.Data;

@Data
@Entity
public class BlueprintLocationRelationship {
    @PrimaryKey
    private int id;
}
