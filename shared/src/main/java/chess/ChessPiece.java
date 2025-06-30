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
        List<ChessMove> moves = new ArrayList<>();
        int row = myPosition.getRow();
        int col = myPosition.getColumn();

        PieceType type = this.getPieceType();

        int[][] directions;
        int maxSteps = 8; // Default for sliders

        if (type == PieceType.ROOK) {
            directions = new int[][] { {1,0}, {-1,0}, {0,1}, {0,-1} };
        } else if (type == PieceType.BISHOP) {
            directions = new int[][] { {1,1}, {1,-1}, {-1,1}, {-1,-1} };
        } else if (type == PieceType.QUEEN) {
            directions = new int[][] { {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1} };
        } else if (type == PieceType.KING) {
            directions = new int[][] { {1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1} };
            maxSteps = 1; 
        } else {
            directions = null;
        }

        if (directions != null) {
            for (int[] dir : directions) {
                int r = row, c = col;
                for (int step = 1; step <= maxSteps; step++) {
                    r += dir[0];
                    c += dir[1];
                    if (r < 1 || r > 8 || c < 1 || c > 8) break;
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
            }
        }
        // Knight
        else if (type == PieceType.KNIGHT) {
            int[] dRows = {-2, -1, 1, 2, 2, 1, -1, -2};
            int[] dCols = {1, 2, 2, 1, -1, -2, -2, -1};
            for (int i = 0; i < 8; i++) {
                int newRow = row + dRows[i];
                int newCol = col + dCols[i];
                if (newRow >= 1 && newRow <= 8 && newCol >= 1 && newCol <= 8) {
                    ChessPosition newPos = new ChessPosition(newRow, newCol);
                    ChessPiece pieceAtNewPos = board.getPiece(newPos);
                    if (pieceAtNewPos == null || pieceAtNewPos.getTeamColor() != this.getTeamColor()) {
                        moves.add(new ChessMove(myPosition, newPos, null));
                    }
                }
            }
        }
        // Pawn (special case)
        else if (type == PieceType.PAWN) {
            int direction = (this.getTeamColor() == ChessGame.TeamColor.WHITE) ? 1 : -1;
            int startRow = (this.getTeamColor() == ChessGame.TeamColor.WHITE) ? 2 : 7;
            int finalRow = (this.getTeamColor() == ChessGame.TeamColor.WHITE) ? 8 : 1;
            int forwardRow = row + direction;
            if (forwardRow >= 1 && forwardRow <= 8) {
                ChessPosition forwardOne = new ChessPosition(forwardRow, col);
                if (board.getPiece(forwardOne) == null) {
                    if (forwardRow == finalRow) {
                        addPromotions(moves, myPosition, forwardOne);
                    } else {
                        moves.add(new ChessMove(myPosition, forwardOne, null));
                    }
                    if (row == startRow) {
                        int forwardTwoRow = row + 2 * direction;
                        ChessPosition forwardTwo = new ChessPosition(forwardTwoRow, col);
                        if (forwardTwoRow >= 1 && forwardTwoRow <= 8 && board.getPiece(forwardTwo) == null) {
                            moves.add(new ChessMove(myPosition, forwardTwo, null));
                        }
                    }
                }
            }
            int[] dCols = {-1, 1};
            for (int dc : dCols) {
                int newCol = col + dc;
                if (newCol >= 1 && newCol <= 8 && forwardRow >= 1 && forwardRow <= 8) {
                    ChessPosition diagonal = new ChessPosition(forwardRow, newCol);
                    ChessPiece target = board.getPiece(diagonal);
                    if (target != null && target.getTeamColor() != this.getTeamColor()) {
                        if (forwardRow == finalRow) {
                            addPromotions(moves, myPosition, diagonal);
                        } else {
                            moves.add(new ChessMove(myPosition, diagonal, null));
                        }
                    }
                }
            }
        }
        return moves;
    }

    private void addPromotions(List<ChessMove> moves, ChessPosition from, ChessPosition to) {
        moves.add(new ChessMove(from, to, ChessPiece.PieceType.QUEEN));
        moves.add(new ChessMove(from, to, ChessPiece.PieceType.ROOK));
        moves.add(new ChessMove(from, to, ChessPiece.PieceType.BISHOP));
        moves.add(new ChessMove(from, to, ChessPiece.PieceType.KNIGHT));
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
