package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.List;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {
    //part of step 4
    private ChessGame.TeamColor pieceColor;
    private PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        //part of step 4
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        //part of step 4
        return pieceColor;

    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {

        //part of step 4
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        //step 7
        List<ChessMove> moves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        if (this.getPieceType() == PieceType.ROOK) {
            // Move right (increase col)
            for (int c = col + 1; c <= 8; c++) {
                ChessPosition newPos = new ChessPosition(row, c);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // Move left (decrease col)
            for (int c = col - 1; c >= 1; c--) {
                ChessPosition newPos = new ChessPosition(row, c);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // Move up (increase row)
            for (int r = row + 1; r <= 8; r++) {
                ChessPosition newPos = new ChessPosition(r, col);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // Move down (decrease row)
            for (int r = row - 1; r >= 1; r--) {
                ChessPosition newPos = new ChessPosition(r, col);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
        } else if (this.getPieceType() == PieceType.BISHOP) {
            // up-right (increase row, increase col)
            for (int r = row + 1, c = col + 1; r <= 8 && c >= 1; r++, c++) {
                ChessPosition newPos = new ChessPosition(r, c);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // up-left (increase row, decrease col)
            for (int r = row + 1, c = col - 1; r <= 8 && c >= 1; r++, c--) {
                ChessPosition newPos = new ChessPosition(r, c);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // down-right (decrease row, increase col)
            for (int r = row - 1, c = col + 1; r >= 1 && c <= 8; r--, c++) {
                ChessPosition newPos = new ChessPosition(r, c);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // down-left (decrease row, decrease col)
            for (int r = row - 1, c = col - 1; r >= 1 && c >= 1; r--, c--) {
                ChessPosition newPos = new ChessPosition(r, c);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
        } else if (this.getPieceType() == PieceType.QUEEN) {
            // Move right (increase col)
            for (int c = col + 1; c <= 8; c++) {
                ChessPosition newPos = new ChessPosition(row, c);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // Move left (decrease col)
            for (int c = col - 1; c >= 1; c--) {
                ChessPosition newPos = new ChessPosition(row, c);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // Move up (increase row)
            for (int r = row + 1; r <= 8; r++) {
                ChessPosition newPos = new ChessPosition(r, col);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // Move down (decrease row)
            for (int r = row - 1; r >= 1; r--) {
                ChessPosition newPos = new ChessPosition(r, col);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // up-right (increase row, increase col)
            for (int r = row + 1, c = col + 1; r <= 8 && c <= 8; r++, c++) {
                ChessPosition newPos = new ChessPosition(r, c);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // up-left (increase row, decrease col)
            for (int r = row + 1, c = col - 1; r <= 8 && c >= 1; r++, c--) {
                ChessPosition newPos = new ChessPosition(r, c);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // down-right (decrease row, increase col)
            for (int r = row - 1, c = col + 1; r >= 1 && c <= 8; r--, c++) {
                ChessPosition newPos = new ChessPosition(r, c);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
            // down-left (decrease row, decrease col)
            for (int r = row - 1, c = col - 1; r >= 1 && c >= 1; r--, c--) {
                ChessPosition newPos = new ChessPosition(r, c);
                ChessPiece pieceAtNewPos = board.getPiece(newPos);
                if (pieceAtNewPos == null) {
                    moves.add(new ChessMove(myPosition, newPos, null));
                } else {
                    if (pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                    break;
                }
            }
        } else if (this.getPieceType() == PieceType.KING) {
            // All 8 directions: up, down, left, right, and diagonals (just one square)
            int[] dRows = {-1, -1, -1,  0, 0, 1, 1, 1};
            int[] dCols = {-1,  0,  1, -1, 1,-1, 0, 1};
            for (int i = 0; i < 8; i++) {
                int newRow = row + dRows[i];
                int newCol = col + dCols[i];
                // Check bounds (1-based indexing)
                if (newRow >= 1 && newRow <= 8 && newCol >= 1 && newCol <= 8) {
                    ChessPosition newPos = new ChessPosition(newRow, newCol);
                    ChessPiece pieceAtNewPos = board.getPiece(newPos);
                    // If the target square is empty or has an enemy piece, add as a move
                    if (pieceAtNewPos == null || pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                }
            }
        }

        return moves;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece that = (ChessPiece) o;
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }

}
