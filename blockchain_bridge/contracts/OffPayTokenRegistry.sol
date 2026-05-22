// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

contract OffPayTokenRegistry {
    enum TokenState {
        NONE,
        AVAILABLE,
        USED,
        RETURNED
    }

    address public owner;

    mapping(bytes32 => TokenState) private tokenStates;

    event TokenRegistered(bytes32 indexed tokenHash);
    event TokenUsed(bytes32 indexed tokenHash);
    event TokenReturned(bytes32 indexed tokenHash);

    error NotOwner();
    error InvalidTokenHash();
    error TokenAlreadyExists();
    error TokenNotAvailable();

    constructor() {
        owner = msg.sender;
    }

    modifier onlyOwner() {
        if (msg.sender != owner) {
            revert NotOwner();
        }
        _;
    }

    function registerToken(bytes32 tokenHash) external onlyOwner {
        if (tokenHash == bytes32(0)) {
            revert InvalidTokenHash();
        }

        if (tokenStates[tokenHash] != TokenState.NONE) {
            revert TokenAlreadyExists();
        }

        tokenStates[tokenHash] = TokenState.AVAILABLE;
        emit TokenRegistered(tokenHash);
    }

    function markTokenUsed(bytes32 tokenHash) external onlyOwner {
        if (tokenStates[tokenHash] != TokenState.AVAILABLE) {
            revert TokenNotAvailable();
        }

        tokenStates[tokenHash] = TokenState.USED;
        emit TokenUsed(tokenHash);
    }

    function markTokenReturned(bytes32 tokenHash) external onlyOwner {
        if (tokenStates[tokenHash] != TokenState.AVAILABLE) {
            revert TokenNotAvailable();
        }

        tokenStates[tokenHash] = TokenState.RETURNED;
        emit TokenReturned(tokenHash);
    }

    function getTokenStatus(bytes32 tokenHash) external view returns (TokenState) {
        return tokenStates[tokenHash];
    }

    function getTokenStatusNumber(bytes32 tokenHash) external view returns (uint8) {
        return uint8(tokenStates[tokenHash]);
    }
}