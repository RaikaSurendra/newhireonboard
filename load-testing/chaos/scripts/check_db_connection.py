#!/usr/bin/env python3
"""
Script to check database connection health
Used by Chaos Toolkit experiments
"""
import mysql.connector
import sys
import os

def check_database_connection():
    """Check if database connection is available"""
    try:
        connection = mysql.connector.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            port=int(os.getenv('DB_PORT', '3306')),
            user=os.getenv('DB_USER', 'root'),
            password=os.getenv('DB_PASSWORD', 'password'),
            database=os.getenv('DB_NAME', 'onboard_buddy'),
            connection_timeout=5
        )
        
        if connection.is_connected():
            cursor = connection.cursor()
            cursor.execute("SELECT 1")
            result = cursor.fetchone()
            cursor.close()
            connection.close()
            
            if result and result[0] == 1:
                print("Database connection successful")
                return True
        
        return False
        
    except mysql.connector.Error as error:
        print(f"Database connection failed: {error}")
        return False
    except Exception as e:
        print(f"Unexpected error: {e}")
        return False

if __name__ == "__main__":
    success = check_database_connection()
    sys.exit(0 if success else 1)
