# Performance tests (GZIP vs Brotli)

## Proto schema
```protobuf
syntax = "proto3";
message uidCollection {
  message uidObject {
    string bidder = 1;
    string uid = 2;
    uint32 expirationOffset = 3;
  }
  uint64 expirationBase = 1;
  repeated uidObject uids = 2;
}
```

## Results
- Used Protobuf + ${compressionType} + Base64
- Used uids of 70 length 
```
prebid.compression.brotli
size (bytes): 3636
time (nanos): 257578.0


prebid.compression.gzip
size (bytes): 3116
time (nanos): 556795.0
```
