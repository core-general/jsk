# Outer Core

Generic messenger bot framework — provides a platform-agnostic messenger API interface and a full graph-based conversation flow engine.

## What It Solves

- `OutMessengerApi` — generic interface for messenger platforms: send (text, image, documents, buttons), edit, delete
- Graph engine (`MgcGraph`, `MgcGraphExecutor`) — full state-machine framework for conversational bots built on JGraphT
- Supports nested sub-graphs, meta-edges (global navigation), back-edges, any-text edges (catch-all)
- Listener system for extensible hooks on node entry, edge traversal, and history updates

## Key Details

- 53 Java files in the graph package alone — this is a substantial state machine engine
- Platform-agnostic: used with Facebook Messenger, Telegram, or any `OutMessengerApi` implementation
- Conversation history tracked per nesting level
