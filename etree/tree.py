import math
from dataclasses import dataclass, field


class Vec3:
    __slots__ = ('x', 'y', 'z')

    def __init__(self, x=0.0, y=0.0, z=0.0):
        self.x = float(x)
        self.y = float(y)
        self.z = float(z)

    def __add__(self, other):
        return Vec3(self.x + other.x, self.y + other.y, self.z + other.z)

    def __sub__(self, other):
        return Vec3(self.x - other.x, self.y - other.y, self.z - other.z)

    def __mul__(self, scalar):
        return Vec3(self.x * scalar, self.y * scalar, self.z * scalar)

    def __rmul__(self, scalar):
        return self.__mul__(scalar)

    def __neg__(self):
        return Vec3(-self.x, -self.y, -self.z)

    def __repr__(self):
        return f"Vec3({self.x:.2f}, {self.y:.2f}, {self.z:.2f})"

    def length(self):
        return math.sqrt(self.x * self.x + self.y * self.y + self.z * self.z)

    def normalized(self):
        ln = self.length()
        if ln < 1e-10:
            return Vec3(0, 1, 0)
        return Vec3(self.x / ln, self.y / ln, self.z / ln)

    def dot(self, other):
        return self.x * other.x + self.y * other.y + self.z * other.z

    def cross(self, other):
        return Vec3(
            self.y * other.z - self.z * other.y,
            self.z * other.x - self.x * other.z,
            self.x * other.y - self.y * other.x,
        )

    def to_tuple(self):
        return (self.x, self.y, self.z)


@dataclass
class Bud:
    phyllotactic_angle: float  # radians
    vigor: float  # 0-1
    position: Vec3
    node_id: int
    age: int = 0  # years since formation


@dataclass
class Node:
    id: int
    position: Vec3
    parent_segment_id: int | None = None
    child_segment_ids: list[int] = field(default_factory=list)
    buds: list[Bud] = field(default_factory=list)


@dataclass
class Segment:
    id: int
    start_node_id: int
    end_node_id: int
    length: float  # cm
    diameter: float  # cm
    direction: Vec3  # unit vector
    age: int = 0  # years
    status: str = 'growing'  # 'growing' | 'mature'
    vigor: float = 1.0  # 0-1
    downstream_leaf_area: float = 0.0


class TreeState:
    def __init__(self):
        self.nodes: dict[int, Node] = {}
        self.segments: dict[int, Segment] = {}
        self.root_node_id: int = 0
        self.current_year: int = 0
        self._next_node_id: int = 0
        self._next_segment_id: int = 0

    def add_node(self, position: Vec3, parent_segment_id: int | None = None) -> Node:
        nid = self._next_node_id
        self._next_node_id += 1
        node = Node(id=nid, position=position, parent_segment_id=parent_segment_id)
        self.nodes[nid] = node
        return node

    def add_segment(self, start_node_id: int, end_node_id: int, length: float,
                    direction: Vec3, vigor: float = 1.0) -> Segment:
        sid = self._next_segment_id
        self._next_segment_id += 1
        seg = Segment(
            id=sid,
            start_node_id=start_node_id,
            end_node_id=end_node_id,
            length=length,
            diameter=0.1,
            direction=direction,
            vigor=vigor,
        )
        self.segments[sid] = seg
        self.nodes[start_node_id].child_segment_ids.append(sid)
        self.nodes[end_node_id].parent_segment_id = sid
        return seg

    def get_terminal_nodes(self) -> list[Node]:
        """Nodes with no child segments (branch tips)."""
        return [n for n in self.nodes.values() if not n.child_segment_ids]

    def get_segment_parent_vigor(self, node_id: int) -> float:
        """Get the vigor of the segment leading into this node."""
        node = self.nodes[node_id]
        if node.parent_segment_id is None:
            return 1.0
        return self.segments[node.parent_segment_id].vigor
