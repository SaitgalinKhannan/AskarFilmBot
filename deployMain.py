import os
import paramiko
import zipfile
import shutil

server_ip = "213.199.32.218"
server_username = "root"
server_password = "thairai702"

source_file = 'build/libs/AskarFilmBot-1.0.1-all.jar'
dest_dir = 'build/AskarFilmBot'

if not os.path.exists(dest_dir):
    os.makedirs(dest_dir)

dest_file = os.path.join(dest_dir, os.path.basename(source_file))
shutil.copy2(source_file, dest_file)

zip_filename = 'AskarFilmBot.zip'
with zipfile.ZipFile(zip_filename, 'w') as zip_file:
    for root, dirs, files in os.walk(dest_dir):
        for file in files:
            zip_file.write(os.path.join(root, file), os.path.relpath(os.path.join(root, file), os.path.join(dest_dir, '..')))

# 3. Отправка ZIP-архива на сервер по SCP
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(server_ip, username=server_username, password=server_password)

scp = ssh.open_sftp()
scp.put(zip_filename, f'/root/{zip_filename}')
scp.close()

stdin, stdout, stderr = ssh.exec_command(f'sudo unzip -o /root/{zip_filename} -d /root/')
stdout.channel.recv_exit_status()

stdin, stdout, stderr = ssh.exec_command('sudo systemctl restart askarbot.service')
stdout.channel.recv_exit_status()

# Закрытие SSH-соединения
ssh.close()

print("Выполнено успешно")