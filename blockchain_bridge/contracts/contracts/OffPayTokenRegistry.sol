// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract OffPayTokenRegistry {
    enum TokenState {
        NONE,
        AVAILABLE,
        USED,
        RETURNED
    }

    struct TokenRecord {
        TokenState state;
        uint256 registeredAt;
        uint256 usedAt;
        uint256 returnedAt;
    }

    address public owner;

    mapping(bytes32 => TokenRecord) private tokens;

    event TokensRegistered(bytes32[] tokenHashes, uint256 timestamp);
    event TokensUsed(bytes32[] tokenHashes, uint256 timestamp);
    event TokensReturned(bytes32[] tokenHashes, uint256 timestamp);
    event OwnershipTransferred(address indexed previousOwner, address indexed newOwner);

    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this function");
        _;
    }

    constructor() {
        owner = msg.sender;
    }

    function transferOwnership(address newOwner) external onlyOwner {
        require(newOwner != address(0), "Invalid new owner");
        emit OwnershipTransferred(owner, newOwner);
        owner = newOwner;
    }

    function registerTokens(bytes32[] calldata tokenHashes) external onlyOwner {
        require(tokenHashes.length > 0, "Empty token list");

        for (uint256 i = 0; i < tokenHashes.length; i++) {
            bytes32 tokenHash = tokenHashes[i];
            require(tokenHash != bytes32(0), "Invalid token hash");
            require(tokens[tokenHash].state == TokenState.NONE, "Token already registered");

            tokens[tokenHash] = TokenRecord({
                state: TokenState.AVAILABLE,
                registeredAt: block.timestamp,
                usedAt: 0,
                returnedAt: 0
            });
        }

        emit TokensRegistered(tokenHashes, block.timestamp);
    }

    function useTokens(bytes32[] calldata tokenHashes) external onlyOwner {
        require(tokenHashes.length > 0, "Empty token list");

        for (uint256 i = 0; i < tokenHashes.length; i++) {
            bytes32 tokenHash = tokenHashes[i];
            require(tokenHash != bytes32(0), "Invalid token hash");
            require(tokens[tokenHash].state == TokenState.AVAILABLE, "Token is not available");

            tokens[tokenHash].state = TokenState.USED;
            tokens[tokenHash].usedAt = block.timestamp;
        }

        emit TokensUsed(tokenHashes, block.timestamp);
    }

    function returnTokens(bytes32[] calldata tokenHashes) external onlyOwner {
        require(tokenHashes.length > 0, "Empty token list");

        for (uint256 i = 0; i < tokenHashes.length; i++) {
            bytes32 tokenHash = tokenHashes[i];
            require(tokenHash != bytes32(0), "Invalid token hash");
            require(tokens[tokenHash].state == TokenState.AVAILABLE, "Only available tokens can be returned");

            tokens[tokenHash].state = TokenState.RETURNED;
            tokens[tokenHash].returnedAt = block.timestamp;
        }

        emit TokensReturned(tokenHashes, block.timestamp);
    }

    function getTokenState(bytes32 tokenHash) external view returns (uint8) {
        return uint8(tokens[tokenHash].state);
    }

    function getTokenRecord(bytes32 tokenHash)
        external
        view
        returns (
            uint8 state,
            uint256 registeredAt,
            uint256 usedAt,
            uint256 returnedAt
        )
    {
        TokenRecord memory record = tokens[tokenHash];
        return (
            uint8(record.state),
            record.registeredAt,
            record.usedAt,
            record.returnedAt
        );
    }

    function isRegistered(bytes32 tokenHash) external view returns (bool) {
        return tokens[tokenHash].state != TokenState.NONE;
    }

    function isAvailable(bytes32 tokenHash) external view returns (bool) {
        return tokens[tokenHash].state == TokenState.AVAILABLE;
    }

    function isUsed(bytes32 tokenHash) external view returns (bool) {
        return tokens[tokenHash].state == TokenState.USED;
    }

    function isReturned(bytes32 tokenHash) external view returns (bool) {
        return tokens[tokenHash].state == TokenState.RETURNED;
    }
}