// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract IouSettlement {
    enum Status { None, Issued, Settled }

    struct Iou {
        address issuer;
        address beneficiary;
        uint256 amount;
        bytes32 currency;
        Status status;
    }

    mapping(bytes32 => Iou) private ious;

    event IouIssued(bytes32 indexed id, address indexed issuer,
        address indexed beneficiary, uint256 amount, bytes32 currency);
    event IouSettled(bytes32 indexed id, address indexed settledBy);

    function issueIou(bytes32 id, address beneficiary, uint256 amount,
            bytes32 currency) external {
        require(id != bytes32(0), "invalid id");
        require(beneficiary != address(0), "invalid beneficiary");
        require(amount > 0, "amount must be positive");
        require(ious[id].status == Status.None, "IOU already exists");
        ious[id] = Iou(msg.sender, beneficiary, amount, currency, Status.Issued);
        emit IouIssued(id, msg.sender, beneficiary, amount, currency);
    }

    function settleIou(bytes32 id) external {
        Iou storage item = ious[id];
        require(item.status == Status.Issued, "IOU is not outstanding");
        require(msg.sender == item.issuer || msg.sender == item.beneficiary,
            "not an IOU party");
        item.status = Status.Settled;
        emit IouSettled(id, msg.sender);
    }

    function getIou(bytes32 id) external view returns (
        address issuer,
        address beneficiary,
        uint256 amount,
        bytes32 currency,
        Status status
    ) {
        Iou storage item = ious[id];
        require(item.status != Status.None, "IOU not found");
        return (item.issuer, item.beneficiary, item.amount, item.currency, item.status);
    }
}
