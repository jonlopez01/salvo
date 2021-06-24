package com.example.salvo.services;

import com.example.salvo.models.Ship;

public interface ShipService {
    Ship findByType(String type);
}
