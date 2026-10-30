# One common semantic decision owner

Workbench owns native SemanticFormationWork and its detached optimistic revision, immutable decision events and formation reservations. Hub remains semantic owner and accesses the same authorized work via child Operations. Studio stores no decisions. Public generic Entity mutation of SemanticFormationWork is denied; all edits pass SemanticWork operations. Application-level hashes, locks, TTLs and a second approval lifecycle are excluded. Independent review, real PostgreSQL qualification and actual artifact delivery remain parent workflow obligations.
