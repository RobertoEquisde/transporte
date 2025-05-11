package com.adavec.transporte.security;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}
