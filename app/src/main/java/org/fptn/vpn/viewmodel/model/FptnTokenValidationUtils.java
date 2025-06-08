package org.fptn.vpn.viewmodel.model;

import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

public class FptnTokenValidationUtils {

    public static void validate(FptnToken token) throws PVNClientException {
        if (token == null) {
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR, "Token cannot be null");
        }

        // Validate username
        if (token.getUsername() == null || token.getUsername().isBlank()) {
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR, "Username cannot be blank");
        }

        // Validate password
        if (token.getPassword() == null || token.getPassword().isBlank()) {
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR, "Password cannot be blank");
        }

        // Validate servers
        if (token.getServers() != null) {
            for (FptnTokenServer server : token.getServers()) {
                validate(server);
            }
        }

        // Validate censoredServers
        if (token.getCensoredServers() != null) {
            for (FptnTokenServer server : token.getCensoredServers()) {
                validate(server);
            }
        }
    }

    public static void validate(FptnTokenServer server) throws PVNClientException {
        if (server == null) {
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR, "Server cannot be null");
        }

        // Validate name
        if (server.getName() == null || server.getName().isBlank()) {
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR, "Server name cannot be blank");
        }
        if (server.getName().length() > 100) {
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR, "Server name must be less than 100 characters");
        }

        // Validate host
        if (server.getHost() == null || server.getHost().isBlank()) {
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR, "Host cannot be blank");
        }

        // Validate md5Fingerprint
        if (server.getMd5Fingerprint() == null || server.getMd5Fingerprint().isBlank()) {
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR, "MD5 fingerprint cannot be blank");
        }

        // Validate port
        if (server.getPort() == null) {
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR, "Port cannot be null");
        }
        if (server.getPort() < 1 || server.getPort() > 65535) {
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR, "Port must be between 1 and 65535");
        }
    }

}
