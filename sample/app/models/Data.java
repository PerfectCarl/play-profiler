package models;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class Data extends Model {

    public Data(long longData) {
        string = longData + "";
    }

    public String string;
}
