// 用户列表页面
'use client';

import { useState } from 'react';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { Header } from '@/components/layout/Header';
import { CreateUserDialog } from '@/components/users/CreateUserDialog';
import { EditUserDialog } from '@/components/users/EditUserDialog';
import { DeleteUserDialog } from '@/components/users/DeleteUserDialog';
import { BatchOperationDialog } from '@/components/users/BatchOperationDialog';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Checkbox } from '@/components/ui/checkbox';
import { UserInfo } from '@/lib/auth/token';
import { useUsers } from '@/hooks/api/useUsers';
import type { CreateUserRequest, UpdateUserRequest, FrontendUser } from '@/types/auth';

// 将 FrontendUser 转换为 UserInfo（兼容现有组件）
const toUserInfo = (user: FrontendUser): UserInfo => ({
  id: user.id,
  username: user.username,
  email: user.email,
  role: user.role,
  createdAt: user.createdAt,
});

const getRoleColor = (role: string) => {
  switch (role) {
    case 'admin':
      return 'bg-red-100 text-red-800';
    case 'user':
      return 'bg-blue-100 text-blue-800';
    case 'viewer':
      return 'bg-gray-100 text-gray-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getRoleName = (role: string) => {
  switch (role) {
    case 'admin':
      return '管理员';
    case 'user':
      return '用户';
    case 'viewer':
      return '查看者';
    default:
      return role;
  }
};

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

function UsersContent() {
  // 使用 API hook 获取用户数据
  const {
    users,
    loading: isLoading,
    error: fetchError,
    pagination,
    createUser: apiCreateUser,
    updateUser: apiUpdateUser,
    deleteUser: apiDeleteUser,
    refresh,
  } = useUsers();

  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(10);
  const [sortField, setSortField] = useState<keyof UserInfo>('createdAt');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
  const [selectedUsers, setSelectedUsers] = useState<string[]>([]);

  // 创建用户处理函数
  const handleCreateUser = async (userData: Omit<UserInfo, 'id' | 'createdAt'> & { password: string }) => {
    // 调用后端 API 创建用户
    const createRequest: CreateUserRequest = {
      userName: userData.username,
      password: userData.password,
      userNick: userData.username, // 使用用户名作为昵称
      sex: 'M', // 默认性别
    };
    
    await apiCreateUser(createRequest);
    console.log('User created successfully via API');
  };

  // 编辑用户处理函数
  const handleEditUser = async (updatedUser: UserInfo) => {
    // 调用后端 API 更新用户
    const updateRequest: UpdateUserRequest = {
      userNick: updatedUser.username,
    };
    
    await apiUpdateUser(parseInt(updatedUser.id), updateRequest);
    console.log('User edited successfully via API:', updatedUser);
  };

  // 删除用户处理函数
  const handleDeleteUser = async (userId: string) => {
    // 调用后端 API 删除用户
    await apiDeleteUser(parseInt(userId));
    // 从选中列表中移除
    setSelectedUsers(prev => prev.filter(id => id !== userId));
    console.log('User deleted successfully via API:', userId);
  };

  // 批量删除用户处理函数
  const handleBatchDeleteUsers = async (userIds: string[]) => {
    // 逐个调用删除 API
    for (const userId of userIds) {
      await apiDeleteUser(parseInt(userId));
    }
    // 清空选中列表
    setSelectedUsers([]);
    console.log('Batch delete users successfully via API:', userIds);
  };

  // 批量更新用户角色处理函数 (后端暂不支持角色更新，保留模拟逻辑)
  const handleBatchUpdateUsersRole = async (userIds: string[], newRole: UserInfo['role']) => {
    // 后端暂不支持角色更新，这里仅刷新数据
    await refresh();
    setSelectedUsers([]);
    console.log('Batch update user roles (simulated):', { userIds, newRole });
  };

  // 单个用户选择处理
  const handleUserSelect = (userId: string, checked: boolean) => {
    if (checked) {
      setSelectedUsers(prev => [...prev, userId]);
    } else {
      setSelectedUsers(prev => prev.filter(id => id !== userId));
    }
  };

  // 全选/取消全选处理
  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedUsers(currentUsers.map(user => user.id));
    } else {
      setSelectedUsers([]);
    }
  };

  // 批量操作完成后的回调
  const handleBatchOperationComplete = () => {
    setSelectedUsers([]);
  };

  // 筛选和排序逻辑
  const filteredUsers = users
    .filter(user => {
      const matchesSearch = user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
                           (user.email || '').toLowerCase().includes(searchTerm.toLowerCase());
      const matchesRole = roleFilter === 'all' || user.role === roleFilter;
      return matchesSearch && matchesRole;
    })
    .sort((a, b) => {
      const aValue = a[sortField as keyof typeof a];
      const bValue = b[sortField as keyof typeof b];
      if (aValue === undefined || bValue === undefined) return 0;
      const comparison = aValue < bValue ? -1 : aValue > bValue ? 1 : 0;
      return sortDirection === 'asc' ? comparison : -comparison;
    });

  // 分页逻辑
  const totalPages = Math.ceil(filteredUsers.length / pageSize);
  const startIndex = (currentPage - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  const currentUsers = filteredUsers.slice(startIndex, endIndex);

  const handleSort = (field: keyof UserInfo) => {
    if (field === sortField) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  };

  const handleSearch = (value: string) => {
    setSearchTerm(value);
    setCurrentPage(1); // 重置到第一页
  };

  const handleRoleFilter = (value: string) => {
    setRoleFilter(value);
    setCurrentPage(1); // 重置到第一页
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  return (
    <div className="min-h-screen bg-background">
      {/* 页面头部操作区域 */}
      <div className="bg-card border-b">
        <div className="container">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-card-foreground">用户管理</h1>
              <p className="text-sm text-muted-foreground">管理系统用户和权限</p>
            </div>
            <div className="flex items-center space-x-4">
              <Button variant="outline" onClick={refresh} disabled={isLoading}>
                {isLoading ? '加载中...' : '刷新'}
              </Button>
              <CreateUserDialog onCreateUser={handleCreateUser} />
              <BatchOperationDialog
                selectedUsers={users.filter(user => selectedUsers.includes(user.id)).map(toUserInfo)}
                onBatchDelete={handleBatchDeleteUsers}
                onBatchUpdateRole={handleBatchUpdateUsersRole}
                onBatchOperationComplete={handleBatchOperationComplete}
              />
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="container py-6">
        {/* 错误提示 */}
        {fetchError && (
          <Card className="mb-6 bg-red-50 border-red-200">
            <CardContent className="py-4">
              <p className="text-red-600">{fetchError}</p>
            </CardContent>
          </Card>
        )}

        {/* 统计卡片 */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-gray-600">总用户数</CardTitle>
              <span className="text-2xl">👥</span>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{isLoading ? '-' : pagination.totalElements}</div>
              <p className="text-xs text-gray-500">系统注册用户</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-gray-600">管理员</CardTitle>
              <span className="text-2xl">🔑</span>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {isLoading ? '-' : users.filter(u => u.role === 'admin').length}
              </div>
              <p className="text-xs text-gray-500">拥有系统管理权限</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-gray-600">普通用户</CardTitle>
              <span className="text-2xl">👤</span>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {isLoading ? '-' : users.filter(u => u.role === 'user').length}
              </div>
              <p className="text-xs text-gray-500">基本使用权限</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-gray-600">查看者</CardTitle>
              <span className="text-2xl">👀</span>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {isLoading ? '-' : users.filter(u => u.role === 'viewer').length}
              </div>
              <p className="text-xs text-gray-500">仅可查看数据</p>
            </CardContent>
          </Card>
        </div>

        {/* 搜索和筛选 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>搜索和筛选</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col sm:flex-row gap-4">
              <div className="flex-1">
                <Input
                  placeholder="搜索用户名或邮箱..."
                  value={searchTerm}
                  onChange={(e) => handleSearch(e.target.value)}
                  className="w-full"
                />
              </div>
              <Select value={roleFilter} onValueChange={handleRoleFilter}>
                <SelectTrigger className="w-[180px]">
                  <SelectValue placeholder="选择角色" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有角色</SelectItem>
                  <SelectItem value="admin">管理员</SelectItem>
                  <SelectItem value="user">用户</SelectItem>
                  <SelectItem value="viewer">查看者</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* 用户列表 */}
        <Card>
          <CardHeader>
            <CardTitle>用户列表</CardTitle>
            <p className="text-sm text-gray-600">
              共 {filteredUsers.length} 个用户
              {searchTerm && ` (搜索: "${searchTerm}")`}
              {roleFilter !== 'all' && ` (角色: ${getRoleName(roleFilter)})`}
            </p>
          </CardHeader>
          <CardContent>
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-12">
                      <Checkbox
                        checked={currentUsers.length > 0 && currentUsers.every(user => selectedUsers.includes(user.id))}
                        onCheckedChange={(checked: boolean) => handleSelectAll(checked)}
                      />
                    </TableHead>
                    <TableHead
                      className="cursor-pointer hover:bg-gray-50"
                      onClick={() => handleSort('username')}
                    >
                      用户名 {sortField === 'username' && (sortDirection === 'asc' ? '↑' : '↓')}
                    </TableHead>
                    <TableHead
                      className="cursor-pointer hover:bg-gray-50"
                      onClick={() => handleSort('email')}
                    >
                      邮箱 {sortField === 'email' && (sortDirection === 'asc' ? '↑' : '↓')}
                    </TableHead>
                    <TableHead
                      className="cursor-pointer hover:bg-gray-50"
                      onClick={() => handleSort('role')}
                    >
                      角色 {sortField === 'role' && (sortDirection === 'asc' ? '↑' : '↓')}
                    </TableHead>
                    <TableHead
                      className="cursor-pointer hover:bg-gray-50"
                      onClick={() => handleSort('createdAt')}
                    >
                      创建时间 {sortField === 'createdAt' && (sortDirection === 'asc' ? '↑' : '↓')}
                    </TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {currentUsers.map((user) => (
                    <TableRow key={user.id} className={selectedUsers.includes(user.id) ? 'bg-blue-50' : ''}>
                      <TableCell>
                        <Checkbox
                          checked={selectedUsers.includes(user.id)}
                          onCheckedChange={(checked: boolean) => handleUserSelect(user.id, checked)}
                        />
                      </TableCell>
                      <TableCell className="font-medium">{user.username}</TableCell>
                      <TableCell>{user.email}</TableCell>
                      <TableCell>
                        <Badge className={getRoleColor(user.role)}>
                          {getRoleName(user.role)}
                        </Badge>
                      </TableCell>
                      <TableCell>{formatDate(user.createdAt)}</TableCell>
                      <TableCell>
                        <div className="flex space-x-2">
                          <EditUserDialog
                            user={toUserInfo(user)}
                            onEditUser={handleEditUser}
                          />
                          <DeleteUserDialog
                            user={toUserInfo(user)}
                            onDeleteUser={handleDeleteUser}
                          />
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>

            {/* 分页 */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-4">
                <p className="text-sm text-gray-700">
                  显示第 {startIndex + 1} - {Math.min(endIndex, filteredUsers.length)} 条，共 {filteredUsers.length} 条
                </p>
                <div className="flex space-x-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 1}
                  >
                    上一页
                  </Button>
                  <span className="px-4 py-2 text-sm">
                    第 {currentPage} 页，共 {totalPages} 页
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage === totalPages}
                  >
                    下一页
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  );
}

export default function UsersPage() {
  return (
    <ProtectedRoute>
      <Header />
      <UsersContent />
    </ProtectedRoute>
  );
}
