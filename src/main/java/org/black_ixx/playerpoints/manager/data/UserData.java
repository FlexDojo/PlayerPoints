package org.black_ixx.playerpoints.manager.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@AllArgsConstructor
@Setter
public class UserData {

    private final UUID uuid;
    private final String name;
    private double points;

}
