package com.sanya.client.security;

import java.security.PublicKey;

public interface KeyRepository {
    void store(String username, PublicKey pubkey);
    PublicKey get(String username);
}
